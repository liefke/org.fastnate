package org.fastnate.generator.test;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Describes the persistence unit for the tests.
 *
 * @author Tobias Liefke
 */
@Getter
@RequiredArgsConstructor
public class TestPersistenceUnitInfo implements PersistenceUnitInfo {

	private final String persistenceUnitName;

	private final String persistenceProviderClassName;

	private final List<String> managedClassNames;

	private final Properties properties;

	@Override
	public void addTransformer(final ClassTransformer transformer) {
		// Ignore
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return false;
	}

	@Override
	public ClassLoader getClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	@Override
	public List<URL> getJarFileUrls() {
		return Collections.emptyList();
	}

	@Override
	public DataSource getJtaDataSource() {
		return null;
	}

	@Override
	public List<String> getMappingFileNames() {
		return Collections.emptyList();
	}

	@Override
	public ClassLoader getNewTempClassLoader() {
		return null;
	}

	@Override
	public DataSource getNonJtaDataSource() {
		return null;
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return getClass().getProtectionDomain().getCodeSource().getLocation();
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {
		return "3.0";
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return null;
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return PersistenceUnitTransactionType.RESOURCE_LOCAL;
	}

	@Override
	public ValidationMode getValidationMode() {
		return null;
	}

}
