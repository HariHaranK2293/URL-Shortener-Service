# Contributing to URL Shortener Service

First off, thank you for considering contributing to the URL Shortener Service! 🎉

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the issue list as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps which reproduce the problem**
- **Provide specific examples to demonstrate the steps**
- **Describe the behavior you observed**
- **Explain which behavior you expected to see instead and why**
- **Include logs and error messages**
- **Include your environment details** (OS, Java version, etc.)

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please include:

- **Use a clear and descriptive title**
- **Provide a step-by-step description of the suggested enhancement**
- **Provide specific examples to demonstrate the steps**
- **Describe the current behavior and expected behavior**
- **Explain why this enhancement would be useful**

### Pull Requests

- Fill in the pull request template
- Follow the Java/Spring Boot style guide
- Include appropriate test cases
- Update documentation if needed
- End all files with a newline

## Development Setup

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- Git

### Setup Steps

1. **Fork and Clone**
```bash
git clone https://github.com/your-username/url-shortener-service.git
cd url-shortener-service
```

2. **Create Feature Branch**
```bash
git checkout -b feature/amazing-feature
```

3. **Start Services**
```bash
docker-compose up -d
```

4. **Build and Test**
```bash
mvn clean install
mvn test
```

5. **Run Application**
```bash
mvn spring-boot:run
```

## Style Guide

### Java Code Style

- **Indentation**: 4 spaces (no tabs)
- **Line Length**: Max 120 characters
- **Naming Convention**:
  - Classes: `PascalCase`
  - Methods/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`

### Class Structure
```java
package com.urlshortener.module;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class MyService {
    
    // Constants
    private static final String CONSTANT = "value";
    
    // Fields
    private final String field;
    
    // Constructor
    public MyService(String field) {
        this.field = field;
    }
    
    // Public Methods
    public void publicMethod() {
        // Implementation
    }
    
    // Private Methods
    private void privateMethod() {
        // Implementation
    }
}
```

### Documentation

- Write clear Javadoc for public classes and methods
- Use meaningful variable names (no single letters except in loops)
- Include comments for complex logic

```java
/**
 * Creates a shortened URL with the provided details.
 *
 * @param request the creation request containing URL and metadata
 * @return the created shortened URL response
 * @throws InvalidUrlException if the URL format is invalid
 */
public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
    // Implementation
}
```

## Testing Guide

### Test Coverage Requirements
- **Service Layer**: 90%+ coverage
- **Controller Layer**: 85%+ coverage
- **Repository Layer**: 80%+ coverage

### Writing Tests

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    
    @Mock
    private MyRepository repository;
    
    @InjectMocks
    private MyService service;
    
    @Test
    void shouldReturnValueWhenConditionMet() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        
        // Act
        Result result = service.getById(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(expectedValue, result.getValue());
    }
}
```

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MyServiceTest

# Run with coverage
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Commit Message Guidelines

Use clear, descriptive commit messages:

```
feat: Add user authentication endpoint

- Implement JWT token generation
- Add API key validation
- Update documentation

Closes #123
```

### Commit Types
- `feat:` A new feature
- `fix:` A bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting)
- `refactor:` Code refactoring
- `perf:` Performance improvements
- `test:` Test additions/updates
- `chore:` Build process, dependencies
- `ci:` CI/CD pipeline changes

## Pull Request Process

1. **Update Documentation** - Update README.md and other docs if needed
2. **Add Tests** - Include tests for new functionality
3. **Run Full Test Suite** - Ensure all tests pass
4. **Update CHANGELOG** - Add entry describing your change
5. **Create PR** - Link related issues
6. **Wait for Review** - Address feedback and comments

### PR Checklist
- [ ] Code builds without errors
- [ ] All tests pass
- [ ] Code follows style guide
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No unnecessary console logs

## Additional Notes

### Issue and Pull Request Labels

- `bug` - Something isn't working
- `enhancement` - New feature or request
- `documentation` - Improvements or additions to documentation
- `good first issue` - Good for newcomers
- `help wanted` - Extra attention is needed
- `question` - Further information is requested
- `wontfix` - This will not be worked on

### Project Structure

```
url-shortener-service/
├── src/
│   ├── main/
│   │   ├── java/com/urlshortener/
│   │   │   ├── config/         # Configuration classes
│   │   │   ├── controller/     # REST endpoints
│   │   │   ├── entity/         # JPA entities
│   │   │   ├── repository/     # Data access layer
│   │   │   ├── service/        # Business logic
│   │   │   ├── dto/            # Data transfer objects
│   │   │   ├── exception/      # Custom exceptions
│   │   │   └── util/           # Utility classes
│   │   └── resources/
│   │       └── application.yml # Configuration
│   └── test/
│       └── java/com/urlshortener/
├── docker-compose.yml          # Docker services
├── Dockerfile                  # Application image
├── pom.xml                     # Maven configuration
└── README.md
```

## Getting Help

- **Documentation**: Check [README.md](README.md)
- **Discussions**: Use GitHub Discussions
- **Issues**: Check existing issues
- **Email**: Contact maintainers

---

Thank you for contributing! 🚀
