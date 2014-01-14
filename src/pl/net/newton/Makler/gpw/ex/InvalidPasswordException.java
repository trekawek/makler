package pl.net.newton.Makler.gpw.ex;

public class InvalidPasswordException extends GpwException {
	private static final long serialVersionUID = -6581241026863936033L;

	public InvalidPasswordException(String msg) {
		super(msg);
	}
}
