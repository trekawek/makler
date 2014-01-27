package pl.net.newton.Makler.db.symbol;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Symbol {
	private final Integer id;

	private final String symbol;

	private final String name;

	private final Boolean isIndex;

	private boolean deleted;

	private final String code;

	Symbol(SymbolBuilder builder) {
		this.id = builder.getId();
		this.symbol = builder.getSymbol();
		this.name = builder.getName();
		this.isIndex = builder.getIsIndex();
		this.deleted = builder.getDeleted();
		this.code = builder.getCode();
	}

	public Integer getId() {
		return id;
	}

	public String getSymbol() {
		return symbol;
	}

	public String getName() {
		return name;
	}

	public Boolean isIndex() {
		return isIndex;
	}

	public boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	@Override
	public String toString() {
		return symbol + " " + name + " " + (isIndex ? "(index)" : "");
	}

	public String getCode() {
		return code;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		Symbol rhs = (Symbol) obj;
		return new EqualsBuilder().append(symbol, rhs.symbol).append(name, rhs.name)
				.append(isIndex, rhs.isIndex).append(deleted, rhs.deleted).append(code, rhs.code).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(symbol).append(name).append(isIndex).append(deleted).append(code)
				.toHashCode();
	}

}
