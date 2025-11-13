package ru.chtcholeg.aichat.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CompositeAgent(
    val type: Type,
) : Agent {

    private val logicScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override val name = "Composite"

    private val aggregatedMessages = MutableStateFlow<List<Message>>(emptyList())
    override val messages = aggregatedMessages.asStateFlow()

    private val masterAgent: Agent by lazy {
        type.createMasterAgent().apply {
            messages
                .map { messages -> messages.filter { it.expectedFormat == ResponseFormat.PLAIN_TEXT } }
                .distinctUntilChanged()
                .onEach { messages ->
                    addNewMessages(messages)
                }.launchIn(logicScope)
        }
    }

    override fun resetMessages() {
        masterAgent.resetMessages()
        aggregatedMessages.value = emptyList()
    }

    override fun addMessage(message: Message) {
        aggregatedMessages.update { it + message }
    }

    override suspend fun processUserRequest(request: String): Result<String> {
        val masterAgentResult = masterAgent.processUserRequest(request)
        val masterAgentContent = masterAgentResult.getOrNull()
        if (masterAgentContent == null) return masterAgentResult

        val agents = try {
            Json.decodeFromString<AgentsDescription>(masterAgentContent)
        } catch (e: Exception) {
            addNewMessage(Message(overriddenContent = e.message))
            return Result.failure(e)
        }

        return askExpertAgents(agents, request)
    }

    private suspend fun askExpertAgents(agentsDescription: AgentsDescription, request: String): Result<String> {
        // Introduction
        val expertsStr = agentsDescription.agents.joinToString { "\"${it.role}\"" }
        addNewMessage(
            Message(
                displayableTitle = "Master agent",
                overriddenContent = "The following experts have been selected: $expertsStr",
            )
        )

        // Experts
        val agentCount = minOf(3, agentsDescription.agents.size)
        val agentResults = mutableListOf<AgentResult>()
        for (i in 0..<agentCount) {
            askExpertAgent(agentsDescription.agents[i], request)
                ?.let { agentResults.add(it) }
        }

        // Result
        return askCollector(agentResults, request)
    }

    private suspend fun askCollector(results: List<AgentResult>, request: String): Result<String> {
        val agent = SingleAgent.custom(
            name = COLLECTOR_AGENT,
            responseFormat = ResponseFormat.PLAIN_TEXT,
            systemPrompt = COLLECTOR_SYSTEM_PROMPT,
        ).apply {
            messages
                .map { messages -> messages.filter { !it.isUserRequest } }
                .distinctUntilChanged()
                .onEach { messages ->
                    addNewMessages(messages)
                }.launchIn(logicScope)
        }

        return agent.processUserRequest(createCollectorPrompt(results, request))
    }

    private fun createCollectorPrompt(results: List<AgentResult>, request: String): String {
        val agentResultsStr = buildString {
            results.forEachIndexed { agentIndex, result ->
                append("Агент №${agentIndex + 1} (роль: ${result.roleName})\n")
                append("${result.content}\n")
            }
        }
        return COLLECTOR_PROMPT_TEMPLATE
            .replace("<<{{user_request}}>>", request)
            .replace("<<{{agent_results}}>>", agentResultsStr)
    }


    private suspend fun askExpertAgent(agentDescription: AgentDescription, request: String): AgentResult? {
        val agent = createExpertAgent(agentDescription).apply {
            messages
                .map { messages -> messages.filter { !it.isUserRequest } }
                .distinctUntilChanged()
                .onEach { messages ->
                    addNewMessages(messages)
                }.launchIn(logicScope)
        }
        agent.processUserRequest(request)
            .onSuccess { content ->
                return@askExpertAgent AgentResult(agentDescription.role, content)
            }
        return null
    }

    private fun createExpertAgent(agentDescription: AgentDescription): Agent {
        return SingleAgent.custom(
            name = agentDescription.role,
            responseFormat = ResponseFormat.PLAIN_TEXT,
            systemPrompt = agentDescription.systemPrompt,
        )
    }

    private fun addNewMessages(messages: List<Message>) {
        aggregatedMessages.update { currentMessages ->
            val existentIds = currentMessages.map { it.id }.toSet()
            val newMessages = messages.filter { !existentIds.contains(it.id) }

            (currentMessages + newMessages).sortedBy { it.id }
        }
    }

    private fun addNewMessage(message: Message) = addNewMessages(listOf(message))

    @Serializable
    private data class AgentDescription(
        val role: String,
        @SerialName("system_prompt") val systemPrompt: String,
    )

    @Serializable
    private data class AgentsDescription(
        val agents: List<AgentDescription>
    )

    private data class AgentResult(
        val roleName: String,
        val content: String,
    )

    sealed interface Type {

        fun createMasterAgent(): Agent

        data object SeveralTaskSolvers : Type {
            override fun createMasterAgent(): Agent =
                SingleAgent.custom(MASTER_AGENT, ResponseFormat.JSON, SEVERAL_TASK_SOLVERS_MASTER_PROMPT)
        }

        companion object {

            val SEVERAL_TASK_SOLVERS_MASTER_JSON_SUBPROMPT = SystemPrompts.json(
                "{\n" +
                        "  \"agents\": [\n" +
                        "    {\n" +
                        "      \"role\": \"краткая_роль\",\n" +
                        "      \"system_prompt\": \"подробный_промпт_для_агента\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
            )

            val SEVERAL_TASK_SOLVERS_MASTER_PROMPT = "Ты - главный координатор AI-агентов.\n" +
                    "Твоя задача:\n" +
                    "1. Проанализировать задачу пользователя\n" +
                    "2. Определить, какие экспертные агенты нужны для её решения. Экспертов ДОЛЖНО БЫТЬ НЕ МЕНЬШЕ 2. Желательное количество количество экспертов - 3-5. \n" +
                    "3. $SEVERAL_TASK_SOLVERS_MASTER_JSON_SUBPROMPT\n" +
                    "\n" +
                    "Примеры ролей: Аналитик, Разработчик, Тестировщик, Архитектор и т.д.\n" +
                    "Каждый системный промпт должен быть достаточно детальным для выполнения конкретной части задачи.\n" +
                    "\"\"\""

        }
    }

    companion object {
        private const val MASTER_AGENT = "Master agent"
        private const val COLLECTOR_AGENT = "Collector agent"

        private const val COLLECTOR_SYSTEM_PROMPT = "# Роль: Синтезатор и Контролёр Качества\n" +
                "\n" +
                "Ты — финальный агент в цепочке принятия решений. Твоя задача — проанализировать решения и мыслительные процессы предыдущих агентов, синтезировать их и выдать пользователю идеальный, целостный и проверенный ответ.\n" +
                "\n" +
                "КЛЮЧЕВЫЕ ПРИНЦИПЫ\n" +
                "\n" +
                "1.  **Анализ, а не Генерация с Нуля:** Твоя основа — это работы предыдущих агентов. Ты не должен придумывать новую информацию \"из головы\", если на то нет веской причины.\n" +
                "2.  **Приоритет Качества над Скоростью:** Твоя главная ценность — найти и исправить ошибки, противоречия и пробелы в цепочке рассуждений.\n" +
                "3.  **Объективность и Критическое Мышление:** Не принимай выводы предыдущих агентов на веру. Всегда спрашивай: \"Это логично? Это подтверждается фактами? Здесь нет предвзятости?\"\n" +
                "4.  **Синтез и Целостность:** Ты не просто перечисляешь ответы других агентов. Ты объединяешь их в единый, связный и структурированный нарратив, который легко понять пользователю.\n" +
                "\n" +
                "ПРОЦЕСС РАБОТЫ\n" +
                "\n" +
                "Получив запрос, ты должен последовательно выполнить следующие шаги:\n" +
                "\n" +
                "1.  **Понимание Исходной Цели:** Внимательно прочти первоначальный запрос пользователя. Это твой ориентир. Всё, что ты делаешь, должно служить этой цели.\n" +
                "2.  **Анализ Входных Данных:** Изучи ответы каждого из предыдущих агентов.\n" +
                "    *   **Что каждый агент сделал правильно?** Выдели сильные стороны.\n" +
                "    *   **Где есть пробелы, неточности или логические ошибки?** Будь придирчивым рецензентом.\n" +
                "    *   **Есть ли противоречия между агентами?** Если да, тебе нужно их разрешить, опираясь на логику и достоверные данные.\n" +
                "    *   **Полностью ли ответили агенты на запрос пользователя?** Если нет, определи, что упущено.\n" +
                "3.  **Синтез и Построение Ответа:**\n" +
                "    *   Используй лучшие части из работ всех агентов.\n" +
                "    *   Исправь найденные ошибки.\n" +
                "    *   Разреши противоречия и объясни пользователю, почему было принято то или иное решение.\n" +
                "    *   Сструктурируй финальный ответ так, чтобы он был максимально понятным и полезным.\n" +
                "4.  **Формирование Вывода:**\n" +
                "    *   Предоставь четкий, прямой ответ на запрос пользователя.\n" +
                "    *   В конце можешь добавить краткую справку о том, как проходил анализ (например: \"На основе анализа экспертизы Агента-А и данных от Агента-Б, мы пришли к выводу...\"), чтобы показать пользователю надежность процесса.\n" +
                "\n" +
                "ТОН И СТИЛЬ\n" +
                "\n" +
                "Ответ должен быть профессиональным, ясным и дружелюбным. Избегай жаргона, если только он не уместен для запроса. Ты — финальный рубеж, поэтому твой голос — это голос авторитетного и надежного помощника."

        private const val COLLECTOR_PROMPT_TEMPLATE =
            "[СИСТЕМНОЕ_СООБЩЕНИЕ: Следуй своей роли Синтезатора и Контролёра Качества. Ниже приведен исходный запрос пользователя и результаты работы предыдущих агентов.]\n" +
                    "**ИСХОДНЫЙ ЗАПРОС ПОЛЬЗОВАТЕЛЯ:**\n" +
                    "<<{{user_request}}>>\n" +
                    "\n" +
                    "**РЕЗУЛЬТАТЫ РАБОТЫ ПРЕДЫДУЩИХ АГЕНТОВ:**\n" +
                    "<<{{agent_results}}>>\n" +
                    "\n" +
                    "**ТВОЯ ЗАДАЧА:**\n" +
                    "Проанализируй предоставленные материалы, следуя своему системному процессу (понимание цели -> анализ -> синтез -> вывод), и сформируй окончательный, всеобъемлющий ответ для пользователя."
    }
}