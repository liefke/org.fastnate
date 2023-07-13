package org.fastnate.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.fastnate.data.DataChangeDetector;
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

	private static final String SETTINGS_KEY = ImportDataMojo.class.getName() + ".settings";

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

	private static void removeObsoleteFiles(final Properties oldSettings, final Properties newSettings) {
		final String oldOutputFile = oldSettings.getProperty(EntityImporter.OUTPUT_FILE_KEY);
		if (oldOutputFile != null && !oldOutputFile.equals(newSettings.getProperty(EntityImporter.OUTPUT_FILE_KEY))) {
			final File oldFile = new File(oldOutputFile);
			if (oldFile.isFile()) {
				oldFile.delete();
			}
		}

	}

	/** The current build context for incremental builds. */
	@Component
	private BuildContext context;

	/** The POM project. */
	@Component
	private MavenProject project;

	@Component
	private PluginDescriptor descriptor;

	/** Indicates to skip the execution of this plugin, even if it is configured for a phase. */
	@Parameter
	private boolean skip;

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

	/**
	 * One or more SQL files in the data folder, or an SQL snippet itself - to put that <i>before</i> the generated SQL.
	 */
	@Parameter
	private String prefix;

	/**
	 * One or more SQL files in the data folder, or an SQL snippet itself - to put that <i>after</i> the generated SQL.
	 */
	@Parameter
	private String postfix;

	/**
	 * A list of patterns for files which are monitored and start the generation when changed.
	 *
	 * See {@link #changeDetector} for more complicated change detection.
	 *
	 * All patterns are relative to the "src" directory, as Eclipse Bug 508238 prevents patterns relative to the base
	 * directory.
	 */
	@Parameter
	private String[] relevantFiles;

	/**
	 * The implementation class of {@link DataChangeDetector}, to check if a changed file is relevant for SQL
	 * generation.
	 */
	@Parameter
	private String changeDetector;

	/**
	 * Any additional settings for the EntitySqlGenerator, see {@link GeneratorContext} for an overview of available
	 * settings.
	 *
	 * Add a setting with: <code>&lt;setting_name&gt;setting_value&lt;/setting_name&gt;</code>
	 */
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
		return new URLClassLoader(projectClasspathList.toArray(new URL[0]));
	}

	private boolean detectChanges(final Properties newSettings, final File outputFile) {
		// Check settings
		final Properties oldSettings = (Properties) this.context.getValue(SETTINGS_KEY);
		if (!newSettings.equals(oldSettings)) {
			if (oldSettings != null) {
				getLog().debug("detectChanges(): Changed settings");

				// Remove any file that is obsolete now, because the settings have changed
				removeObsoleteFiles(oldSettings, newSettings);
			} else {
				getLog().debug("detectChanges(): No previous run");
			}
			this.context.setValue(SETTINGS_KEY, newSettings.clone());
			return true;
		}

		// Check if the output file was deleted
		if (!outputFile.isFile()) {
			getLog().debug("detectChanges(): Missing output file");
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
			final Scanner dataFolderScanner = this.context
					.newScanner(new File(this.project.getBasedir(), dataFolderPath));
			dataFolderScanner.scan();
			if (dataFolderScanner.getIncludedDirectories().length > 0
					|| dataFolderScanner.getIncludedFiles().length > 0) {
				getLog().debug("detectChanges(): data folder changed");
				return true;
			}
		}

		// Find any changed entity, data provider or other file
		if (detectRelevantFileChanges()) {
			return true;
		}

		// Nothing relevant changed
		getLog().debug("detectChanges(): No changes detected");
		return false;
	}

	private boolean detectFilePropertyChanges(final Properties properties, final String key, final File outputFile) {
		String propertyValue = properties.getProperty(key);
		if (propertyValue == null || (propertyValue = propertyValue.trim()).length() == 0) {
			return false;
		}

		if (propertyValue.endsWith(".sql")) {
			final File baseDir = new File(this.project.getBasedir(),
					properties.getProperty(EntityImporter.DATA_FOLDER_KEY, "src/data"));
			final String[] fileNames = propertyValue.split("[\\n\\" + File.pathSeparatorChar + ",;]+");
			for (final String fileName : fileNames) {
				final File file = new File(baseDir, fileName);
				if (this.context.hasDelta(file) && !this.context.isUptodate(outputFile, file)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean detectRelevantChagesUsingDetector() {
		if (this.changeDetector != null) {
			final List<String> sourceRoots = this.project.getCompileSourceRoots();
			for (final String sourceRoot : sourceRoots) {
				final Scanner sourceFolderScanner = this.context.newScanner(new File(sourceRoot));
				sourceFolderScanner.scan();
				final String[] includedFiles = sourceFolderScanner.getIncludedFiles();
				if (includedFiles.length > 0) {
					try {
						final Class<?> detectorClass = Thread.currentThread().getContextClassLoader()
								.loadClass(this.changeDetector);
						final Object detector = detectorClass.newInstance();
						final Method isDataFile = detectorClass.getMethod("isDataFile", File.class);
						for (final String file : includedFiles) {
							if ((Boolean) isDataFile.invoke(detector, new File(file))) {
								getLog().debug("detectChanges(): change detector fired for " + file);
								return true;
							}
						}
					} catch (final ReflectiveOperationException e) {
						getLog().error("Could not execute DataChangeDetector: " + this.changeDetector, e);
					}
				}
			}
		}

		return false;
	}

	private boolean detectRelevantClassFileChanges() {
		final File outputDirectory = new File(this.project.getBuild().getOutputDirectory());
		final Scanner scanner = this.context.newScanner(outputDirectory);
		scanner.setIncludes(new String[] { "**/*.class" });
		scanner.scan();
		if (scanner.getIncludedFiles().length > 0) {
			// Load classes
			final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			final Class<?> dataProviderClass;
			final Class<? extends Annotation> entityClass;
			final Class<? extends Annotation> embeddableClass;
			final Class<? extends Annotation> mappedSuperClass;
			try {
				dataProviderClass = classLoader.loadClass("org.fastnate.data.DataProvider");
				entityClass = (Class<? extends Annotation>) classLoader.loadClass("jakarta.persistence.Entity");
				embeddableClass = (Class<? extends Annotation>) classLoader.loadClass("jakarta.persistence.Embeddable");
				mappedSuperClass = (Class<? extends Annotation>) classLoader
						.loadClass("jakarta.persistence.MappedSuperclass");
			} catch (final ClassNotFoundException e) {
				getLog().warn("Missing JPA or fastnate-data dependency", e);
				return false;
			}

			for (final String file : scanner.getIncludedFiles()) {
				try {
					// Load class
					final Class<?> c = classLoader.loadClass(
							file.replace('\\', '.').replace('/', '.').substring(0, file.length() - ".class".length()));

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

	private boolean detectRelevantFileChanges() {
		if (detectRelevantSourceFileChanges()) {
			return true;
		}

		if (detectRelevantClassFileChanges()) {
			return true;
		}

		return detectRelevantChagesUsingDetector();
	}

	private boolean detectRelevantSourceFileChanges() {
		if (this.relevantFiles != null && this.relevantFiles.length > 0) {
			// Use src directory, due to Eclipse Bug 508238
			final Scanner scanner = this.context.newScanner(new File(this.project.getBasedir(), "src"));
			scanner.setIncludes(this.relevantFiles);
			scanner.scan();
			if (scanner.getIncludedFiles().length > 0) {
				getLog().debug("detectChanges(): changes detected for relevant file " + scanner.getIncludedFiles()[0]);
				return true;
			}
		}
		return false;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.skip) {
			return;
		}

		// Install correct classpath
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

				// Build the SQL file
				final String outputEncoding = settings.getProperty(EntityImporter.OUTPUT_ENCODING_KEY, "UTF-8");
				getLog().info("Writing entities to " + outputFile + " with encoding " + outputEncoding);
				final File directory = outputFile.getParentFile();
				if (directory != null && !directory.exists()) {
					directory.mkdirs();
				}
				try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
						this.context.newFileOutputStream(outputFile), Charset.forName(outputEncoding)))) {
					final Class<?> importerClass = Thread.currentThread().getContextClassLoader()
							.loadClass("org.fastnate.data.EntityImporter");
					final Object importer = importerClass.getConstructor(settings.getClass()).newInstance(settings);
					importerClass.getMethod("importData", Writer.class).invoke(importer, writer);
				} catch (final InvocationTargetException e) {
					final Throwable target = e.getTargetException();
					getLog().error("Could not generate SQL file: " + this.sqlFile, target);
					throw new MojoExecutionException(
							"Could not generate SQL file '" + this.sqlFile + "' due to " + target, target);
					// CHECKSTYLE OFF: IllegalCatch
				} catch (final IOException | ReflectiveOperationException | RuntimeException e) {
					// CHECKSTYLE ON: IllegalCatch
					getLog().error("Could not generate SQL file: " + this.sqlFile, e);
					throw new MojoExecutionException("Could not generate SQL file '" + this.sqlFile + "' due to " + e,
							e);
				}
			}
		} catch (final IOException e) {
			throw new MojoExecutionException("Could not create class loader: " + e, e);
		} finally {
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
	}
}
