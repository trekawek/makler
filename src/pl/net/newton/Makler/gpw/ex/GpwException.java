package pl.net.newton.Makler.gpw.ex;

public class GpwException extends Exception {
	private static final long serialVersionUID = 8212054254054897460L;

	public GpwException(String message) {
		super(message);
	}

	public GpwException(String message, Throwable cause) {
		super(message, cause);
	}

	public GpwException(Throwable cause) {
		super(cause);
	}
}
