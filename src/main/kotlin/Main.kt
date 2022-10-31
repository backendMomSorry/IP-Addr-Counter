import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.concurrent.atomic.AtomicIntegerArray

const val BIT_IN_INT = 32
const val AMOUNT_OF_ALL_IPS = 134217728
const val CHANNEL_CAPACITY = 50
const val NUMBER_OF_STRINGS_IN_CHANNEL = 1000

suspend fun main(args: Array<String>) {
    val start = System.currentTimeMillis()
    val filePath = args.firstOrNull() ?: run {
        println("Укажите путь к файлу")
        return
    }

    val file = File(filePath)
    if (!file.exists()) {
        println("Файл не найден")
        return
    }

    coroutineScope {
        val channel = Channel<List<String>>(CHANNEL_CAPACITY)

        launchReadFile(file, channel)

        val ips = AtomicIntegerArray(AMOUNT_OF_ALL_IPS)

        withContext(Dispatchers.Default) {
            for (i in 0 until ((Runtime.getRuntime().availableProcessors() - 1).takeIf { it > 1 } ?: 1)) {
                launch {
                    saveUniqueIpsFromFile(channel, ips)
                }
            }
        }

        println("Уникальных ip: ${getNumberOfUniqueIps(ips)}")
        println("Общее время: ${System.currentTimeMillis() - start}")
    }
}

private fun CoroutineScope.launchReadFile(
    file: File,
    channel: Channel<List<String>>
) {
    launch(Dispatchers.IO) {
        val reader = BufferedReader(FileReader(file))
        var line = reader.readLine()
        var lines: MutableList<String> = mutableListOf()
        while (line != null) {
            lines.add(line)

            if (lines.size == NUMBER_OF_STRINGS_IN_CHANNEL) {
                channel.send(lines)
                lines = mutableListOf()
            }

            line = reader.readLine()
        }
        if (lines.isNotEmpty()) {
            channel.send(lines)
        }
        channel.close()
    }
}

private suspend fun saveUniqueIpsFromFile(
    channel: Channel<List<String>>,
    ips: AtomicIntegerArray
) {
    while (true) {

        val channelResult = channel.receiveCatching()
        if (channelResult.isClosed) {
            return
        }

        channelResult.getOrThrow()
            .forEach { ip ->
                val digitInIp = ip
                    .split(".")
                    .map { it.toInt() }
                val index = getIndex(digitInIp)
                val code = getSpecialCode(digitInIp[3] % BIT_IN_INT)

                var result = false
                while (!result) {
                    val int = ips[index]
                    result = ips.compareAndSet(index, int, int or code)
                }
            }
    }
}

private fun getIndex(digitInIp: List<Int>) =
    (digitInIp[0] shl 19) + (digitInIp[1] shl 11) + (digitInIp[2] shl 3) + (digitInIp[3] / BIT_IN_INT)

private fun getNumberOfUniqueIps(ips: AtomicIntegerArray): Int {
    var counter = 0

    for (i in 0 until ips.length()) {
        counter += ips[i].countOneBits()
    }

    return counter
}


fun getSpecialCode(digit: Int): Int {
    if (digit >= BIT_IN_INT) {
        throw Exception("incorrect digit")
    }

    return 1 shl digit
}