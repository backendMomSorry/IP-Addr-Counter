import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.concurrent.atomic.AtomicIntegerArray

const val BIT_IN_INT = 32
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

    val ips = AtomicIntegerArray(134217728)

    coroutineScope {
        val channel = Channel<List<String>>(50)

        launch(Dispatchers.IO) {
            val reader = BufferedReader(FileReader(file))
            var line = reader.readLine()
            var lines: MutableList<String> = mutableListOf()
            while (line != null) {
                lines.add(line)

                if (lines.size == 1000) {
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

        launch {
            saveUniqueIpsFromFile(channel, ips)
        }

        launch {
            saveUniqueIpsFromFile(channel, ips)
        }

        launch {
            saveUniqueIpsFromFile(channel, ips)
        }

        withContext((Dispatchers.Default)) {
            saveUniqueIpsFromFile(channel, ips)
        }

        println("Уникальных ip: ${getNumberOfUniqueIps(ips)}")
        println("Общее время: ${System.currentTimeMillis() - start}")
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
            .forEach {
                val digitInIp = it
                    .split(".")
                    .map { it.toInt() }
                val index = getIndex(digitInIp)
                val binaryCode = getBinaryCode(digitInIp[3] % BIT_IN_INT)

                var result = false
                while (!result) {
                    val byte = ips[index]
                    result = ips.compareAndSet(index, byte, byte or binaryCode)
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


fun getBinaryCode(digit: Int): Int {
    if (digit > 31) {
        throw Exception("incorrect digit")
    }

    return 0b00000001 shl digit
}