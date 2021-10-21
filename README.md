# Dataflow Analysis

This project contains the source code for my master's thesis "Methoden zur automatischen Dokumentation von Datenmappings zwischen klinischen Informationsmodellen in Java am Beispiel von FHIR und openEHR".
It was previously developed in another repository and migrated to GitHub
afterwards.

## Description
To support the fight against the SARS-CoV-2 pandemic and as future pandemics or waves, a secure, extensible and interoperable platform for the provision of  research data on COVID-19 is required.
As part of the *Network University Medicine* project, the FHIR-Bridge interface was developed to transfer and validate data from the FHIR format into the openEHR clinical model.
The interface will be used to merge medical data from different university hospitals in Germany.
During this process, the details for the mapping of the data are recorded in a manual documentation.
The creation of the necessary documents, is a labor-intensive and time-consuming task that is very vulnerable to errors.

We developed a system for an automated generation of software documentation for the FHIR-Bridge project.
The project is split into an extraction component, which extracts information from the source code, and a visualization component, which displays the documentation on a website.
Such a split allows the visualization component to be used for other applications as well.
The automatically generated documentations achieved an accuracy of 97.14% compared to the manually generated documentations which achieved only an accuracy of 91.31%.
Furthermore, the presentation of the documentations through the website, was enhanced with additional features such as a search function that further improved usability and clarity.

Health data is usually very complex, so creating, storing and maintaining the data is very time-consuming and expensive.
When exchanging this medical information, it is often necessary to convert the data into other formats.
The developed prototype is the first system for automatic documentation generation in this area and shows that similar approaches can be usefully applied in medical informatics.

## Documentation Server

The *documentation* folder contains a Django webserver that is responsible for displaying a documentation.
The project was developed and tested with Python 3.8 but probably work on older and newer versions, too.
To set up the project go into the `documentation` folder and install the project dependencies.

```
pip install -r requirements.txt
```

Afterwards you can initialize the SQLite database and load a dummy dataset.

```
python manage.py migrate
python load_mapping.py -f db_dump.json
```

You can now run the server with the following command and access it through http://localhost:8000

```
python manage.py runserver
```

## Extraction Component

The extraction component is implemented in Java and mainly uses
the [JavaParser](https://github.com/javaparser/javaparser) library for code analysis.
Due to time constraints towards the end of my thesis, the code is not well documented 
and contains several hard-coded paths that hinder a straightforward execution.
The code is highly dependent on the [FHIRBridge](https://github.com/ehrbase/fhir-bridge) project that was
updated several times after my thesis. 
The core data flow functionality can be found at
`com.dataflow.analysis` while the control flow and other important data structure generation was done within `com.dataflow.generation`.
In order to export the results into a json file for the documentation the `com.dataflow.exportable` package was used.
Please refer to my thesis for more detail on the implementation.