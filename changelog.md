Fastnate Changelog
------------------

### 1.0.0-RC1 (2016-05-31)
* First release
* Support for JPA 2.1 and Hibernate 4.3.5

### 1.1.0-RC1 (2016-06-02)
* Aligned to Wildfly 10
* Support for Hibernate 5.0.7
** Changed default naming stratey for id column in collection mappings
** Changed default GenerationType for H2
* Dropped support for Java 7 (only Java 8 is supported now)
* Write update statements for @OneToMany(mappedBy="...") with @OrderColumn
