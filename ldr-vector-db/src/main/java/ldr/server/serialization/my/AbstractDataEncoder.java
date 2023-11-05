package ldr.server.serialization.my;

import java.util.List;

/**
 * В этом классе имеютсю полезные вспомогательные функции для енкодеров. Отнаследуйтесь, при необходимости.
 */
public abstract class AbstractDataEncoder<D> implements DataEncoder<D> {
    /**
     * Put encoded data to byte array list.
     */
    protected static <E> int putToList(List<byte[]> bytesList, E toEncode, DataEncoder<E> coder) {
        byte[] result = coder.encode(toEncode);
        bytesList.add(result);

        return result.length;
    }

    protected static void unwrapList(byte[] result, List<byte[]> bytesList) {
        int pos = 0;
        for (byte[] bytes : bytesList) {
            System.arraycopy(bytes, 0, result, pos, bytes.length);
            pos += bytes.length;
        }
    }
}
