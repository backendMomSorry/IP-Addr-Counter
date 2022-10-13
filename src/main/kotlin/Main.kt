import java.io.*
import kotlin.experimental.or

const val MAX_DIGIT_IP = 256
const val BIT_IN_BYTE = 8

fun main(args: Array<String>) {
    val filePath = args.firstOrNull() ?: run {
        println("Укажите путь к файлу")
        return
    }

    val file = File(filePath)
    if (!file.exists()) {
        println("Файл не найден")
        return
    }

    val reader = BufferedReader(FileReader(file))
    var line = reader.readLine()

    val ips = Array(MAX_DIGIT_IP) {
        Array(MAX_DIGIT_IP) {
            Array(MAX_DIGIT_IP) {
                ByteArray(MAX_DIGIT_IP / BIT_IN_BYTE)
            }
        }
    }

    while (line != null) {
        val digitInIp = line.split(".").map { it.toInt() }
        val lastIndex = digitInIp[3] / BIT_IN_BYTE
        val binaryCode = getBinaryCode(digitInIp[3] % BIT_IN_BYTE)

        val byte = ips[digitInIp[0]][digitInIp[1]][digitInIp[2]][lastIndex]
        ips[digitInIp[0]][digitInIp[1]][digitInIp[2]][lastIndex] = byte or binaryCode

        line = reader.readLine()
    }

    println("Уникальных ip: ${getNumberOfUniqueIps(ips)}")
}

private fun getNumberOfUniqueIps(ips: Array<Array<Array<ByteArray>>>): Int {
    var counter = 0
    ips.forEach {
        it.forEach {
            it.forEach {
                it.forEach { byte ->
                    counter += byte.countOneBits()
                }
            }
        }
    }
    return counter
}


fun getBinaryCode(digit: Int): Byte = when (digit) {
    0 -> 0b00000001
    1 -> 0b00000010
    2 -> 0b00000100
    3 -> 0b00001000
    4 -> 0b00010000
    5 -> 0b00100000
    6 -> 0b01000000
    7 -> -0b10000000
    else -> throw Exception("incorrect digit")
}