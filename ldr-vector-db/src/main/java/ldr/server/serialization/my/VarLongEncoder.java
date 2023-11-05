package ldr.server.serialization.my;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class VarLongEncoder implements DataEncoder<Long> {
    // 10000000
    static final int CONTINUE_BIT = 0x80;
    private static final long MAX_VALUE = Long.MAX_VALUE / 2;
    private static final long MIN_VALUE = Long.MIN_VALUE / 2;
    // 01111111
    private static final byte SEGMENT_BITS = 0x7F;

    @Override
    public byte[] encode(Long value) {
        // We cast long to unsigned vector long, so diapason is smaller than long in 2 times.
        if (value > MAX_VALUE || value < MIN_VALUE) {
            throw new RuntimeException("Value incorrect diapason should be between " +
                    MIN_VALUE + " and " + MAX_VALUE + ", your vector " + value);
        }

        // Zig Zag
        long unsignedValue = (value << 1) ^ (value >> 63);

        List<Byte> encodedBytes = new ArrayList<>();
        //пока есть байты
        while (unsignedValue > 0) {
            // В каждом байте храним 7 бит числа и бит продолжения
            byte b = (byte) (unsignedValue & SEGMENT_BITS);
            unsignedValue >>= 7;
            // Устанавливаем бит продолжения, если есть ещё группы для кодирования
            if (unsignedValue > 0) {
                b |= CONTINUE_BIT;
            }
            encodedBytes.add(b);
        }
        // Если байты закончились
        if (encodedBytes.isEmpty()) {
            encodedBytes.add((byte) 0);
        }

        // Преобразуем список байтов в массив байтов и возвращаем как результат
        byte[] result = new byte[encodedBytes.size()];
        for (int i = 0; i < encodedBytes.size(); i++) {
            result[i] = encodedBytes.get(i);
        }
        return result;
    }

    @Override
    public DecodeResult<Long> decode(byte[] encodedBytes, int from) {
        return decode(from, encodedBytes.length, i -> encodedBytes[i]);
    }

    @Override
    public DecodeResult<Long> decode(ByteBuffer byteBuffer, int from) {
        return decode(from, byteBuffer.limit(), byteBuffer::get);
    }

    private DecodeResult<Long> decode(int from, int limit, Function<Integer, Byte> positionGetter) {
        long result = 0;
        byte shift = 0;

        int bytesCount;
        for (bytesCount = 0; bytesCount < limit; bytesCount++) {
            byte byteValue = positionGetter.apply(bytesCount + from);
            // Очищаем бит продолжения и объединяем с результатом
            result |= (long) (byteValue & SEGMENT_BITS) << shift;

            // Сдвиг на 7 бит для обработки следующего байта
            shift += 7;

            // Если бит продолжения равен 0, это последний байт числа
            if ((byteValue & CONTINUE_BIT) == 0) {
                break;
            }
        }
        // First byte was in zero index
        bytesCount += 1;

        if (bytesCount > Long.BYTES + 1) {
            throw new RuntimeException("Too many bytes in VarLong");
        }

        // Раскодирование zig zag
        return new DecodeResult<>((result >> 1) ^ (-(result & 1)), bytesCount);
    }
}

