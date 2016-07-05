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

### 1.2.0 (xxx-xxx-xxx)
* Better support for MySQL
* #7 Support for GenerationType.TABLE
* #9 fixed wrong ID calculation for InheritanceType.JOINED
* #10 Support for referencing subclasses with InheritanceType.JOINED 
* #11 Support for @Version
