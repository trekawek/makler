package pl.net.newton.Makler.history;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public final class ChannelTools {
	private ChannelTools() {
	}

	public static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest)
			throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
		while (src.read(buffer) != -1) {
			// prepare the buffer to be drained
			buffer.flip();
			// write to the channel, may block
			dest.write(buffer);
			// If partial transfer, shift remainder down
			// If buffer is empty, same as doing clear()
			buffer.compact();
		}
		// EOF will leave buffer in fill state
		buffer.flip();
		// make sure the buffer is fully drained.
		while (buffer.hasRemaining()) {
			dest.write(buffer);
		}
	}

	public static void copy(InputStream is, OutputStream os) throws IOException {
		final ReadableByteChannel inputChannel = Channels.newChannel(is);
		final WritableByteChannel outputChannel = Channels.newChannel(os);
		// copy the channels
		ChannelTools.fastChannelCopy(inputChannel, outputChannel);
		// closing the channels
		inputChannel.close();
		outputChannel.close();
	}
}