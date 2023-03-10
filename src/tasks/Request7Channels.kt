package tasks

import contributors.*
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    coroutineScope {
        val repos = service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .body() ?: listOf()
        var allUsers = emptyList<User>()
        val userChannel = Channel<List<User>>(UNLIMITED)
        repos.forEach { repo ->
            launch {
                service.getRepoContributors(req.org, repo.name).body()?.let { userChannel.send(it) }
            }
        }
        repeat(repos.size) {
            val users = userChannel.receive()
            allUsers = (allUsers + users).aggregate()
            updateResults(allUsers, false)
        }
        updateResults(allUsers, true)
    }
}
