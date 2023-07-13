package org.fastnate.generator.test;

import java.io.IOException;
import java.io.Writer;

import jakarta.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes SQL to an {@link EntityManager}.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
@Slf4j
public class SqlEmWriter extends Writer {

	private final EntityManager em;

	private final StringBuilder currentLine = new StringBuilder();

	private int index = 0;

	@Override
	public void close() throws IOException {
		if (this.currentLine.length() > 0) {
			write('\n');
		}
	}

	@Override
	public void flush() throws IOException {
		// Ignore
	}

	@Override
	public void write(final char[] cbuf, final int off, final int len) throws IOException {
		for (this.currentLine.append(cbuf, off, len); this.index < this.currentLine.length(); this.index++) {
			if (this.currentLine.charAt(this.index) == '\n') {
				if (this.index > 0) {
					this.em.getTransaction().begin();
					try {
						final String sql = StringUtils.removeEnd(this.currentLine.substring(0, this.index), ";");
						log.info("Writing SQL: {}", sql);
						this.em.createNativeQuery(sql).executeUpdate();
					} finally {
						if (!this.em.getTransaction().getRollbackOnly()) {
							this.em.getTransaction().commit();
						}
					}
				}
				this.currentLine.delete(0, this.index + 1);
				this.index = 0;
			}
		}
	}

}
