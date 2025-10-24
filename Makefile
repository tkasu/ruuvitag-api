.PHONY: help build test lint format clean run console

help:
	@echo "Ruuvitag API - Available targets:"
	@echo "  make build   - Compile project"
	@echo "  make test    - Run all tests"
	@echo "  make lint    - Check code formatting"
	@echo "  make format  - Format code with scalafmt"
	@echo "  make clean   - Remove build artifacts"
	@echo "  make run     - Run application (when implemented)"
	@echo "  make console - Start SBT console"

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
	@echo "Note: HTTP server not yet implemented"
	sbt run

console:
	sbt console
