package pl.net.newton.Makler.gpw.ex;

public class InvalidPasswordException extends Exception {
	private static final long serialVersionUID = -6581241026863936033L;

	private String msg;

	public InvalidPasswordException(String msg) {
		this.msg = msg;
	}

	@Override
	public String toString() {
		return this.msg;
	}
}
