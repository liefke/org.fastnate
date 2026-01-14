package org.fastnate.examples.data;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.annotation.Resource;

import org.fastnate.data.AbstractDataProvider;
import org.fastnate.data.EntityRegistration;
import org.fastnate.data.csv.CsvDataImporter;
import org.fastnate.data.files.DataFolder;
import org.fastnate.data.properties.FormatConverter;
import org.fastnate.data.properties.MapConverter;
import org.fastnate.examples.model.Organisation;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;
import org.supercsv.comment.CommentStartsWith;
import org.supercsv.prefs.CsvPreference;

import lombok.Getter;

/**
 * Example DataProvider for importing the CSV file src/main/data/organisations.csv.
 *
 * @author Tobias Liefke
 */
@Getter
public class OrganisationData extends AbstractDataProvider {

	@Resource
	private DataFolder dataFolder;

	@Resource
	private GeneratorContext context;

	@Resource
	private EntityRegistration entityRegistration;

	private final Map<String, Organisation> organisations = new LinkedHashMap<>();

	@Override
	public void buildEntities() throws IOException {
		// Build our own CSV settings
		final CsvPreference csvSettings = new CsvPreference.Builder(CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE)
				.skipComments(new CommentStartsWith("#")).build();

		// Lookup the definition of the entity class.
		final EntityClass<Organisation> entityClass = this.context.getDescription(Organisation.class);

		// Create the importer
		// we inject the global EntityRegistration to be able to reference the imported organisations in the generic imports
		final CsvDataImporter<Organisation> importer = new CsvDataImporter<>(entityClass, csvSettings,
				this.entityRegistration);

		// Map the "id" and "name" column with the default mappings
		importer.addDefaultColumnMapping("id");
		importer.addDefaultColumnMapping("name");

		// Map the "web" column to the "url" property
		importer.addColumnMapping("web", Organisation::setUrl);

		// Add a currency converter for the profit column
		importer.addColumnMapping("profit", float.class,
				new FormatConverter<>(NumberFormat.getCurrencyInstance(Locale.US)), Organisation::setProfit);

		// Add a lookup for the parent column
		importer.addColumnMapping("parent", MapConverter.create(this.organisations), Organisation::setParent);

		// Ignore the comment column
		importer.addIgnoredColumn("comment");

		// Store each entity in the map of organisations
		importer.addPostProcessor(organisation -> this.organisations.put(organisation.getName(), organisation));

		importer.importFile(this.dataFolder.findFile("organisations.csv"));
	}

	@Override
	protected Collection<?> getEntities() {
		return this.organisations.values();
	}

}
