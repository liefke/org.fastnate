package org.fastnate.generator.context;

import java.util.Collections;
import java.util.List;

import javax.persistence.TableGenerator;

import org.apache.commons.lang.StringUtils;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.provider.JpaProvider;
import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;
import org.fastnate.generator.statements.TableStatement;
import org.fastnate.generator.statements.UpdateStatement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Saves the current value for a {@link TableGenerator}.
 *
 * @author Tobias Liefke
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TableIdGenerator extends IdGenerator {

	private final GeneratorDialect dialect;

	private final int initialValue;

	private final int allocationSize;

	private final String generatorTable;

	private final String pkColumnName;

	private final String pkColumnValue;

	private final String valueColumnName;

	private long nextValue;

	private long currentTableValue;

	/**
	 * Creates a new instance of {@link SequenceIdGenerator}.
	 *
	 * @param generator
	 *            the annotation that contains our settings
	 * @param dialect
	 *            the dialect of the database
	 * @param provider
	 *            the provider of the target JPA framework
	 */
	public TableIdGenerator(final TableGenerator generator, final GeneratorDialect dialect,
			final JpaProvider provider) {
		this.dialect = dialect;
		this.allocationSize = generator.allocationSize();
		this.currentTableValue = this.initialValue = generator.initialValue();
		this.nextValue = this.initialValue;
		this.generatorTable = StringUtils.defaultIfEmpty(generator.table(), provider.getDefaultGeneratorTable());
		this.valueColumnName = StringUtils.defaultIfEmpty(generator.valueColumnName(),
				provider.getDefaultGeneratorTableValueColumnName());
		this.pkColumnName = StringUtils.defaultIfEmpty(generator.pkColumnName(),
				provider.getDefaultGeneratorTablePkColumnName());
		final String value = StringUtils.defaultIfEmpty(generator.pkColumnValue(),
				provider.getDefaultGeneratorTablePkColumnValue());
		this.pkColumnValue = StringUtils.isEmpty(value) ? null : dialect.quoteString(value);
	}

	@Override
	public void addNextValue(final InsertStatement statement, final String column, final Number value) {
		statement.addValue(column, String.valueOf(value));
	}

	@Override
	public List<? extends EntityStatement> alignNextValue() {
		if (this.currentTableValue > this.initialValue && this.currentTableValue > this.nextValue) {
			final UpdateStatement statement = new UpdateStatement(this.generatorTable, this.pkColumnName,
					this.pkColumnValue);
			this.currentTableValue = this.nextValue;
			statement.addValue(this.valueColumnName, String.valueOf(this.currentTableValue + this.allocationSize - 1));
			return Collections.singletonList(statement);
		}
		return Collections.emptyList();
	}

	@Override
	public long createNextValue() {
		return this.nextValue++;
	}

	@Override
	public List<? extends EntityStatement> createPreInsertStatements() {
		if (this.currentTableValue > this.nextValue) {
			return Collections.emptyList();
		}
		final TableStatement statement;
		if (this.currentTableValue == this.initialValue) {
			statement = new InsertStatement(this.generatorTable);
			statement.addValue(this.pkColumnName, this.pkColumnValue);
		} else {
			statement = new UpdateStatement(this.generatorTable, this.pkColumnName, this.pkColumnValue);
		}
		this.currentTableValue += this.allocationSize;
		statement.addValue(this.valueColumnName, String.valueOf(this.currentTableValue + this.allocationSize - 1));
		return Collections.singletonList(statement);
	}

	@Override
	public IdGenerator derive(final String currentTable) {
		if (StringUtils.isEmpty(this.pkColumnValue)) {
			return new TableIdGenerator(this.dialect, this.initialValue, this.allocationSize, this.generatorTable,
					this.pkColumnName, this.dialect.quoteString(currentTable), this.valueColumnName, this.nextValue,
					this.currentTableValue);
		}
		return this;
	}

	@Override
	public long getCurrentValue() {
		return this.nextValue - 1;
	}

	@Override
	public String getExpression(final String tableName, final String columnName, final Number targetId,
			final boolean whereExpression) {
		return String.valueOf(targetId);
	}

	@Override
	public boolean isPostIncrement() {
		return false;
	}

}
