Fastnate Changelog
------------------

### 1.0.0-RC1 (2016-05-31)
* First release
* Support for JPA 2.1 and Hibernate 4.3.5

### 1.1.0-RC1 (2016-06-02)
* Aligned to Wildfly 10
* Support for Hibernate 5.0.7
    * Changed default naming stratey for id column in collection mappings
    * Changed default GenerationType for H2
* Dropped support for Java 7 (only Java 8 is supported now)
* Write update statements for @OneToMany(mappedBy="...") with @OrderColumn

### 1.1.0 (2016-06-08)
* Changed how @Column and @Size on one single attribute are interpreted (aligned with Hibernate)

### 1.2.0 (2016-09-06)
* Better support for MySQL
* #7 Support for GenerationType.TABLE
* #9 fixed wrong ID calculation for InheritanceType.JOINED
* #10 Support for referencing subclasses with InheritanceType.JOINED 
* #11 Support for @Version
* #12 Increment generated IDs by one
	* Write absolute IDs by default
	* Full support for relative IDs in all GenerationTypes
* Simplified unit testing against H2, MySQL, PostgreSQL and Oracle

### 1.3.0 (2017-02-24)
* #14 Better support for Microsoft SQL Server
* #15 Configure relevant files without a ChangeDetector enhancement
* #16 Relative dates in SQL files
* #18 Generated relative IDs are wrong for TableGenerator
* #20 Option to write statements directly to the database
* Workaround for Eclipse Bug 508238
* Support "skip" parameter in Maven-Configuration
* Support for null values in entity collections
* Support "byte", "short" resp. "int" IDs as well
* Improved documentation / examples
