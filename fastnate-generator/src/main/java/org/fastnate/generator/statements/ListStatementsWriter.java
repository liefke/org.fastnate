package org.fastnate.generator.statements;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * A simple {@link StatementsWriter}, which just stores all generated statements into a list.
 *
 * @author Tobias Liefke
 */
public class ListStatementsWriter extends AbstractStatementsWriter {

	@Getter
	private final List<String> statements = new ArrayList<>();

	@Override
	public void writeStatement(final EntityStatement statement) {
		this.statements.add(statement.toSql());
	}
}
