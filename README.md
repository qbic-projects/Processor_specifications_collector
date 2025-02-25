# Processor specifications collector
Collects specifications of Intel, AMD and Ampera processors and summarizes them in one file. The programm can be re-run periodically and will only fetch new information after a given time period.

## Features

| Feature                                       | Status |
|-----------------------------------------------|--------|
| Extract processor specifications from Intel   | ✅     |
| Extract processor specifications from AMD     | ✅     |
| Extract processor specifications from Ampera  | ✅     |
| Summarize all specifications into one table   | ✅     |
| Remove units from different properties        | ✅     |
| Configuration via JSON/YAML file              | 🛠️     |
| Conversion of scaled units to base units      | 🛠️     |

## Usage
### Installation
```bash
git clone <REPOSITORY_PATH>
cd <REPOSITORY_NAME>
```

### Execution
From the base directory of the project, run:
```bash
./gradlew run
```

### Customization
Before you run the program you might adapt the following maps:

#### Mapping column aliases
```groovy
static Map<String, String[]> specification_aliases = [
        'product_id': ['name', 'product_id'],
        'name': ['name'],
        'time': ['time'],
        'source': ['source'],
        'tdp': [
            'SDP', 'Scenario Design Power',
            'Processor Base Power', 'USAGE POWER (W)', 'Default TDP',
            'tdp', 'thermal design power',
        ],
        'cores': ['Total Cores', '# of CPU Cores', 'cores'],
        'threads': ['cores', 'Total Cores', '# of CPU Cores', 'Total Threads', '# of Threads', 'threads']
    ]
```
This map is responsible for mapping the information in the parsed files to desired properties. For example, the `tdp` or thermal design power has different names for different model generations and/or manufacturers. If you want to have different properties in your end table, you can adapt the map to contain your desired colum name as a key and the values it corresponds to as a list of values. If multiple values match, the last one will take precedence, so in the example: If `Processor Base Power` is found, but also `thermal design power` has a value assigned in the vendor infomation, the value of `thermal design power` will be used.

#### Mapping units
```groovy
// Mapping units to columns
static Map<String, String[]> units_mapping = ['tdp': ['W', 'Watt']]
```
This map tells the program, which units to expect in the respective columns. It then extracts the first value in the list to the top and removes them from the data. Example:
| tdp |
|-----|
| 1 W |
| 2 Watt|
| 3   |

Will be transformed to:
|tdp (W)|
|-------|
| 1     |
| 2     |
| 3     |

There is currently **no support** to map scaled units to base units (e.g. 1 kW -> 1000 W).

## Contibuting
### Linting
The linting rules are specified in `.groovylintrc.json` please apply them, when contributing new code.

### Testing
From the base directory of the project, run:
```bash
./gradlew test
```
