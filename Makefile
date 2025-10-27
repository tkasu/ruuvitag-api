.PHONY: help build test lint format clean run console e2e clean-db

help:
	@echo "Ruuvitag API - Available targets:"
	@echo "  make build    - Compile project"
	@echo "  make test     - Run all unit tests"
	@echo "  make e2e      - Run end-to-end tests (starts server, makes HTTP requests)"
	@echo "  make lint     - Check code formatting"
	@echo "  make format   - Format code with scalafmt"
	@echo "  make clean    - Remove build artifacts"
	@echo "  make clean-db - Remove development SQLite database"
	@echo "  make run      - Run application"
	@echo "  make console  - Start SBT console"

build:
	sbt compile

test:
	sbt test

lint:
	sbt scalafmtCheckAll

format:
	sbt scalafmtAll

clean:
	sbt clean

run:
	sbt run

e2e:
	@echo "Running end-to-end tests..."
	@chmod +x scripts/e2e-test.sh
	@./scripts/e2e-test.sh

clean-db:
	@echo "Removing development SQLite database..."
	@rm -f data/ruuvitag.db data/ruuvitag.db-journal
	@echo "Database cleaned. The database will be recreated on next application startup."

console:
	sbt console
