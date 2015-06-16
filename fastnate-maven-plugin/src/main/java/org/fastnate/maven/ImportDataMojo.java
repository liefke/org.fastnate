package org.fastnate.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.fastnate.data.DataProvider;
import org.fastnate.data.EntityImporter;
import org.fastnate.generator.context.GeneratorContext;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Creates an SQL file from all {@link DataProvider}.
 *
 * @author Tobias Liefke
 */
@Mojo(name = "import-data", threadSafe = true, defaultPhase = LifecyclePhase.PROCESS_CLASSES, //
requiresDependencyResolution = ResolutionScope.COMPILE)
public class ImportDataMojo extends AbstractMojo {

	private static void addProperty(final Properties settings, final String key, final String value) {
		if (value != null) {
			settings.put(key, value);
		}
	}

	private static boolean isAnnotationPresent(final Class<?> c, final Class<? extends Annotation>... annotations) {
		for (final Class<? extends Annotation> annotation : annotations) {
			if (c.isAnnotationPresent(annotation)) {
				return true;
			}
		}
		return false;
	}

	private static final String SETTINGS_KEY = ImportDataMojo.class.getName() + ".settings";

	/** The current build context for incremental builds. */
	@Component
	private BuildContext context;

	/** The POM project. */
	@Component
	private MavenProject project;

	@Component
	private PluginDescriptor descriptor;

	/** The name of the database dialect. */
	@Parameter
	private String dialect;

	/** The packages to scan for data providers. */
	@Parameter
	private String packages;

	/** The data folder for importers (e.g. to import CSV files). */
	@Parameter(defaultValue = "${basedir}/src/main/data")
	private File dataFolder;

	/** The path to the output file. */
	@Parameter(defaultValue = "${project.build.outputDirectory}/data.sql")
	private File sqlFile;

	/** The encoding of the target and the prefix / postfix files. */
	@Parameter(defaultValue = "UTF-8")
	private String encoding;

	/** The path to a prefix file, or the content of the prefix itself. */
	@Parameter
	private String prefix;

	/** The path to a postfix file, or the content of the postfix itself. */
	@Parameter
	private String postfix;

	/** Any additional settings for the EntitySqlGenerator, see {@link GeneratorContext}. */
	@Parameter
	private Map<String, String> additionalSettings;

	private URLClassLoader buildClassLoader() throws MojoExecutionException {
		final List<URL> projectClasspathList = new ArrayList<>();
		try {
			for (final Object element : this.project.getCompileClasspathElements()) {
				try {
					projectClasspathList.add(new File(element.toString()).toURI().toURL());
				} catch (final MalformedURLException e) {
					throw new MojoExecutionException(element + " is an invalid classpath element", e);
				}
			}
		} catch (final DependencyResolutionRequiredException e) {
			throw new MojoExecutionException("Could not find project dependencies", e);
		}
		return new URLClassLoader(projectClasspathList.toArray(new URL[0]), getClass().getClassLoader());
	}

	private boolean detectChanges(final Properties newSettings, final File outputFile) {
		// Check if the output file was deleted
		if (!outputFile.isFile()) {
			getLog().debug("detectChanges(): Missing output file");
			return true;
		}

		// Check settings
		final Properties oldSettings = (Properties) this.context.getValue(SETTINGS_KEY);
		if (!newSettings.equals(oldSettings)) {
			if (oldSettings != null) {
				getLog().debug("detectChanges(): Changed settings");
			} else {
				getLog().debug("detectChanges(): No previous run");
			}
			this.context.setValue(SETTINGS_KEY, newSettings.clone());
			return true;
		}

		// Check pre-/postfix settings
		if (detectFilePropertyChanges(newSettings, EntityImporter.PREFIX_KEY, outputFile)
				|| detectFilePropertyChanges(newSettings, EntityImporter.POSTFIX_KEY, outputFile)) {
			getLog().debug("detectChanges(): pre-/ or postfix changed");
			return true;
		}

		// Check data files
		final String dataFolderPath = newSettings.getProperty(EntityImporter.DATA_FOLDER_KEY);
		if (dataFolderPath != null) {
			final Scanner dataFolderScanner = this.context.newScanner(new File(dataFolderPath));
			dataFolderScanner.scan();
			if (dataFolderScanner.getIncludedDirectories().length > 0
					|| dataFolderScanner.getIncludedFiles().length > 0) {
				getLog().debug("detectChanges(): data folder changed");
				return true;
			}
		}

		// Find any changed entity of data provider class
		if (detectRelevantClassChanges()) {
			return true;
		}

		getLog().debug("detectChanges(): No changes detected");
		return false;
	}

	private boolean detectFilePropertyChanges(final Properties properties, final String key, final File outputFile) {
		final String fileName = properties.getProperty(key);
		if (fileName == null || fileName.length() == 0) {
			return false;
		}
		final File file = new File(fileName);
		if (!file.isFile()) {
			return false;
		}

		return !this.context.isUptodate(outputFile, file);
	}

	private boolean detectRelevantClassChanges() {
		final File outputDirectory = new File(this.project.getBuild().getOutputDirectory());
		final Scanner scanner = this.context.newScanner(outputDirectory);
		scanner.setIncludes(new String[] { "**/*.class" });
		scanner.scan();
		if (scanner.getIncludedFiles().length > 0) {
			// Load classes
			final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			Class<?> dataProviderClass;
			Class<? extends Annotation> entityClass;
			Class<? extends Annotation> embeddableClass;
			Class<? extends Annotation> mappedSuperClass;
			try {
				dataProviderClass = classLoader.loadClass("org.fastnate.data.DataProvider");
				entityClass = (Class<? extends Annotation>) classLoader.loadClass("javax.persistence.Entity");
				embeddableClass = (Class<? extends Annotation>) classLoader.loadClass("javax.persistence.Embeddable");
				mappedSuperClass = (Class<? extends Annotation>) classLoader
						.loadClass("javax.persistence.MappedSuperclass");
			} catch (final ClassNotFoundException e) {
				getLog().warn("Missing JPA or fastnate-data dependency", e);
				return false;
			}

			for (final String file : scanner.getIncludedFiles()) {
				try {
					// Load class
					final Class<?> c = classLoader.loadClass(file.replace('\\', '.').replace('/', '.')
							.substring(0, file.length() - ".class".length()));

					// Check if the class is a data provider
					if (dataProviderClass.isAssignableFrom(c)) {
						getLog().debug("detectChanges(): data provider changed");
						return true;
					}

					// Check if the class is a JPA class
					if (isAnnotationPresent(c, entityClass, mappedSuperClass, embeddableClass)) {
						getLog().debug("detectChanges(): JPA class changed");
						return true;
					}
				} catch (final ClassNotFoundException e) {
					// Ignore and continue
				}
			}
		}
		return false;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// Install correct classpath
		// We can't use the current class realm of our pluginDescription, as that one is cached in M2E
		final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		try (URLClassLoader classloader = buildClassLoader()) {
			Thread.currentThread().setContextClassLoader(classloader);

			// Generate settings from configuration
			final Properties settings = new Properties();
			addProperty(settings, EntityImporter.DATA_FOLDER_KEY, this.dataFolder.getPath());
			addProperty(settings, EntityImporter.OUTPUT_FILE_KEY, this.sqlFile.getPath());
			addProperty(settings, EntityImporter.OUTPUT_ENCODING_KEY, this.encoding);

			if (this.additionalSettings != null) {
				settings.putAll(this.additionalSettings);
			}

			addProperty(settings, GeneratorContext.DIALECT_KEY, this.dialect);
			addProperty(settings, EntityImporter.PACKAGES_KEY, this.packages);
			addProperty(settings, EntityImporter.PREFIX_KEY, this.prefix);
			addProperty(settings, EntityImporter.POSTFIX_KEY, this.postfix);

			// Scan for changes
			final File outputFile = new File(settings.getProperty(EntityImporter.OUTPUT_FILE_KEY, "data.sql"));
			if (detectChanges(settings, outputFile)) {
				if (settings.getProperty(GeneratorContext.DIALECT_KEY) == null) {
					getLog().warn("No explicit database dialect specified, using default: H2");
					settings.setProperty(GeneratorContext.DIALECT_KEY, "H2Dialect");
				}

				final String outputEncoding = settings.getProperty(EntityImporter.OUTPUT_ENCODING_KEY, "UTF-8");
				getLog().info("Writing entities to " + outputFile + " with encoding " + outputEncoding);
				try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
						this.context.newFileOutputStream(outputFile), Charset.forName(outputEncoding)))) {
					final Class<?> importerClass = Thread.currentThread().getContextClassLoader()
							.loadClass("org.fastnate.data.EntityImporter");
					final Object importer = importerClass.getConstructor(settings.getClass()).newInstance(settings);
					importerClass.getMethod("importData", Writer.class).invoke(importer, writer);
					// CHECKSTYLE OFF: IllegalCatch
				} catch (final IOException | ReflectiveOperationException | RuntimeException e) {
					// CHECKSTYLE ON: IllegalCatch
					throw new MojoExecutionException("Could not generate SQL file: " + this.sqlFile, e);
				}
			}
		} catch (final IOException e) {
			throw new MojoExecutionException("Could not create class loader: " + e, e);
		} finally {
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
	}
}
