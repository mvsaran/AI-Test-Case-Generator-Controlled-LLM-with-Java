# 🤖 AI Test Case Generator — Controlled LLM with Java

> **AI-powered, schema-validated test case generation for QA Engineers and SDETs**
>
> Built with Java 17 · Maven · OpenAI GPT-4o · Jackson · OkHttp · JSON Schema Validation

---

## 📌 Table of Contents

1. [Problem Statement](#-problem-statement)
2. [What Is Controlled LLM?](#-what-is-controlled-llm)
3. [Architecture Flow](#-architecture-flow)
4. [Project Structure](#-project-structure)
5. [Tech Stack](#-tech-stack)
6. [Prerequisites](#-prerequisites)
7. [Step-by-Step Setup](#-step-by-step-setup)
8. [How to Run](#-how-to-run)
9. [Sample Input](#-sample-input)
10. [Sample Output (JSON)](#-sample-output-json)
11. [JSON Schema Explained](#-json-schema-explained)
12. [Schema Validation Flow](#-schema-validation-flow)
13. [Guardrails Enforced](#-guardrails-enforced)
14. [Console Output](#-console-output)
15. [Unit Tests](#-unit-tests)
16. [Benefits for QA Teams](#-benefits-for-qa-teams)
17. [Future: Selenium Integration](#-future-selenium-integration)

---

## 🔴 Problem Statement

In QA, converting requirements into test cases is **repetitive and time-consuming**.

While LLMs like GPT-4 can speed this up, relying on **uncontrolled LLM output** in a QA pipeline is dangerous:

| Risk | Why It's a Problem |
|---|---|
| **Inconsistent format** | Output varies per call — Markdown today, plain text tomorrow |
| **Unstructured** | Cannot be parsed by automation pipelines or CI/CD |
| **Verbose** | "Here are your test cases..." text pollutes the output |
| **Missing coverage** | LLMs focus on happy paths and omit negative/boundary cases |
| **Not validatable** | No schema = no way to verify completeness programmatically |
| **Risky in automation** | Malformed or incomplete test cases silently cause failures |

This project solves all of these by implementing a **Controlled LLM Generation approach**.

---

## 🛡️ What Is Controlled LLM?

**Controlled LLM** means wrapping the AI call with strict rules and automatic validation:

```
Requirement Input
       ↓
Strong Prompt (forces JSON only, no markdown, enum rules)
       ↓
OpenAI GPT-4o API (JSON mode ON)
       ↓
JSON Schema Validation (Draft-07)
       ↓ FAIL?
Correction Prompt → Retry (up to 2 times)
       ↓ PASS
Normalize (dedup, fix enums, reassign IDs)
       ↓
Write Output (JSON + Summary)
```

The result is **machine-readable, schema-compliant, automation-ready** test cases every time.

---

## 🏗️ Architecture Flow

```
┌─────────────────────────────────────────────────────────┐
│                   Main.java (Orchestrator)               │
└──────────┬──────────────────────────────────────────────┘
           │
    ┌──────▼──────┐
    │  Module 1   │  RequirementReader.java
    │  Reader     │  Reads .txt file → normalizes text
    └──────┬──────┘
           │ requirement text
    ┌──────▼──────┐
    │  Module 2   │  PromptBuilder.java
    │  Prompter   │  Builds system prompt + user prompt with
    │             │  schema constraints and guardrails
    └──────┬──────┘
           │ prompt
    ┌──────▼──────┐
    │  Module 3   │  OpenAiClient.java (OkHttp)
    │  API Client │  Calls OpenAI /chat/completions in JSON mode
    └──────┬──────┘
           │ raw JSON string
    ┌──────▼──────┐
    │  Module 5   │  SchemaValidator.java (networknt)
    │  Validator  │  Validates against testcase-schema.json
    └──────┬──────┘
      PASS │      │ FAIL → PromptBuilder.buildCorrectionPrompt()
           │      └──── Retry (max 2 times) ────────────────┐
           │◄───────────────────────────────────────────────┘
    ┌──────▼──────┐
    │  Module 4   │  ResponseParser.java (Jackson)
    │  Parser     │  JSON string → Java model objects
    └──────┬──────┘
           │ TestCaseResponse
    ┌──────▼──────┐
    │  Module 6   │  OutputNormalizer.java
    │  Normalizer │  Dedup · Fix enums · Reassign IDs
    └──────┬──────┘
           │ cleaned response
    ┌──────▼──────┐
    │  Module 7   │  OutputWriter.java
    │  Writer     │  Writes JSON + Summary + Debug files
    └─────────────┘
```

---

## 📂 Project Structure

```
AITESTCASEGENERATOR/                          ← Project Root
│
├── pom.xml                                   ← Maven build config + all dependencies
│
├── .env                                      ← YOUR API KEY (not committed to Git)
├── .env.example                              ← Copy this → .env and add your key
├── .gitignore                                ← Excludes .env, target/, IDE files
│
├── README.md                                 ← This file
│
├── schema/
│   └── testcase-schema.json                  ← Reference copy of the JSON Schema
│
├── src/
│   ├── main/
│   │   ├── java/com/example/aitestgen/
│   │   │   │
│   │   │   ├── Main.java                     ← Entry point · Orchestrates all modules · Retry logic
│   │   │   │
│   │   │   ├── model/                        ← Java POJO models (Jackson-mapped)
│   │   │   │   ├── TestCaseResponse.java     ← Root model: feature_name, requirement_summary, test_scenarios
│   │   │   │   ├── TestScenario.java         ← Single test case: steps, priority, type, flags
│   │   │   │   └── TestData.java             ← Nested: input + notes for each test case
│   │   │   │
│   │   │   ├── reader/
│   │   │   │   └── RequirementReader.java    ← Module 1: Reads .txt files or raw strings, normalizes
│   │   │   │
│   │   │   ├── prompt/
│   │   │   │   └── PromptBuilder.java        ← Module 2: System + User + Correction prompts
│   │   │   │
│   │   │   ├── client/
│   │   │   │   └── OpenAiClient.java         ← Module 3: OkHttp → OpenAI API · JSON mode · Markdown strip
│   │   │   │
│   │   │   ├── parser/
│   │   │   │   └── ResponseParser.java       ← Module 4: Jackson JSON → Java object mapping
│   │   │   │
│   │   │   ├── validator/
│   │   │   │   ├── SchemaValidator.java      ← Module 5: networknt Draft-07 schema validation
│   │   │   │   └── OutputNormalizer.java     ← Module 6: Dedup · Enum fix · ID reassignment · Defaults
│   │   │   │
│   │   │   ├── writer/
│   │   │   │   └── OutputWriter.java         ← Module 7: Writes JSON, summary.txt, debug dump
│   │   │   │
│   │   │   ├── util/
│   │   │   │   └── FileUtils.java            ← Shared: readFile, writeFile, ensureDirectory
│   │   │   │
│   │   │   └── bonus/
│   │   │       └── SeleniumSkeletonGenerator.java  ← BONUS: Converts automation_candidate=true → TestNG Java stubs
│   │   │
│   │   └── resources/
│   │       └── schema/
│   │           └── testcase-schema.json      ← Classpath copy loaded by SchemaValidator at runtime
│   │
│   └── test/
│       └── java/com/example/aitestgen/
│           ├── PromptBuilderTest.java         ← 10 tests: prompt structure, guardrail keywords, enum presence
│           ├── SchemaValidatorTest.java       ← 14 tests: valid JSON, missing fields, bad enums, step count
│           └── GeneratorFlowTest.java         ← 12 tests: parse → validate → normalize end-to-end flow
│
├── requirements/                             ← Sample input requirement files
│   ├── login_requirement.txt                 ← User login with credentials, locking, forgot password
│   ├── registration_requirement.txt          ← New user sign-up with validation rules
│   ├── appointment_booking_requirement.txt   ← Doctor appointments with date/slot selection
│   └── checkout_requirement.txt             ← E-commerce checkout, payment, shipping
│
└── output/                                   ← Generated files (auto-created on run)
    ├── generated_testcases.json             ← Final validated, normalized test cases (JSON)
    ├── generated_testcases.csv              ← Auto-generated, Excel-ready test case grid (CSV)
    ├── summary.txt                          ← Human-readable summary: counts, priorities, IDs
    └── raw_response_debug.txt               ← Only written if all retries fail (for debugging)
```

---

## 🛠 Tech Stack

| Library | Version | Purpose |
|---|---|---|
| **Java** | 17 | Language version (text blocks, records) |
| **Maven** | 3.8+ | Build and dependency management |
| **Jackson Core + Databind** | 2.16.1 | JSON parsing and POJO mapping |
| **OkHttp3** | 4.12.0 | HTTP client for OpenAI API calls |
| **networknt json-schema-validator** | 1.3.3 | JSON Schema Draft-07 validation |
| **dotenv-java** | 3.0.0 | Load `.env` file for API key |
| **SLF4J Simple** | 2.0.12 | Lightweight logging |
| **JUnit Jupiter** | 5.10.2 | Unit testing framework |
| **Mockito** | 5.10.0 | Mocking for unit tests |
| **Maven Shade Plugin** | 3.5.2 | Fat JAR packaging |

---

## ✅ Prerequisites

Before you start, make sure the following are installed on your machine:

1. **Java 17+**
   ```bash
   java -version
   # Should show: openjdk 17.x.x or higher
   ```

2. **Maven 3.8+**
   ```bash
   mvn -version
   # Should show: Apache Maven 3.8.x or higher
   ```

3. **OpenAI API Key**
   - Sign up at [https://platform.openai.com](https://platform.openai.com)
   - Go to **API Keys** → Create a new key
   - Make sure you have access to the **`gpt-4o`** model

4. **Internet connection** (to reach `api.openai.com`)

---

## 🚀 Step-by-Step Setup

### Step 1 — Clone or Open the Project

Open a terminal and navigate to the project root:

```bash
cd c:\Users\mvsar\Projects\AITESTCASEGENERATOR
```

---

### Step 2 — Add Your OpenAI API Key

Create a `.env` file in the project root:

```bash
# On Windows PowerShell:
Copy-Item .env.example .env
```

Open the `.env` file and paste your API key:

```properties
OPENAI_API_KEY=sk-your-actual-openai-api-key-here
```

> ⚠️ **Never commit `.env` to Git.** It is already listed in `.gitignore`.

---

### Step 3 — Download Dependencies and Build

Run Maven clean install to download all dependencies from Maven Central:

```bash
mvn clean install -DskipTests
```

You should see:
```
[INFO] BUILD SUCCESS
```

---

### Step 4 — Run the Unit Tests

Verify the project is working correctly without calling the OpenAI API:

```bash
mvn clean test
```

Expected output:
```
[INFO] Running com.example.aitestgen.GeneratorFlowTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.aitestgen.PromptBuilderTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.aitestgen.SchemaValidatorTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

✅ **36 tests pass out of the box without any API key.**

---

### Step 5 — Run the Generator (Live API Call)

**Option A — Using Maven exec plugin (recommended for development):**

```bash
# Login requirement (default)
mvn exec:java -Dexec.mainClass="com.example.aitestgen.Main"

# Custom requirement file
mvn exec:java -Dexec.mainClass="com.example.aitestgen.Main" -Dexec.args="requirements/registration_requirement.txt"

# Appointment booking
mvn exec:java -Dexec.mainClass="com.example.aitestgen.Main" -Dexec.args="requirements/appointment_booking_requirement.txt"

# Checkout flow
mvn exec:java -Dexec.mainClass="com.example.aitestgen.Main" -Dexec.args="requirements/checkout_requirement.txt"
```

**Option B — Build a fat JAR and run it:**

```bash
# Package into a single executable JAR
mvn clean package -DskipTests

# Run with default requirement (login)
java -jar target/ai-test-case-generator-1.0.0.jar

# Run with a custom requirement file
java -jar target/ai-test-case-generator-1.0.0.jar requirements/registration_requirement.txt
```

---

### Step 6 — Review the Output

After a successful run, check the `output/` folder:

```bash
# View the generated test cases (JSON)
cat output/generated_testcases.json

# View the Excel-ready CSV export
cat output/generated_testcases.csv

# View the summary report
cat output/summary.txt
```

---

## 📄 Sample Input

**`requirements/login_requirement.txt`:**

```text
FEATURE: User Login

REQUIREMENT:
User should be able to log in to the application using a registered email address
and password.

DETAILED BEHAVIOR:
- If credentials are valid, user is redirected to the dashboard.
- If credentials are invalid, a clear error message must be shown.
- Password field must be masked (hidden characters).
- Login button should be disabled until both fields are filled.
- Maximum 5 failed login attempts allowed; after that, account is locked for 15 minutes.
- Remember Me option should persist the session for 30 days.
- Forgot Password link must be available on the login page.

VALIDATION RULES:
- Email must be a valid format (e.g., user@domain.com).
- Both fields are mandatory.
```

---

## 📤 Sample Output (JSON)

**`output/generated_testcases.json` (excerpt):**

```json
{
  "feature_name": "User Login",
  "requirement_summary": "Registered users log in with email and password. Invalid credentials show errors. Account locks after 5 failed attempts.",
  "assumptions": [
    "The application is accessible via a modern web browser",
    "Users have already completed registration and email verification"
  ],
  "test_scenarios": [
    {
      "test_case_id": "TC_001",
      "title": "Successful login with valid email and password",
      "objective": "Verify that a registered user with valid credentials is redirected to the dashboard.",
      "preconditions": [
        "User is registered and email is verified",
        "Application is accessible",
        "User is on the login page"
      ],
      "test_steps": [
        "Step 1: Navigate to the login page URL",
        "Step 2: Enter a valid registered email address in the email field",
        "Step 3: Enter the correct password in the password field",
        "Step 4: Click the Login button",
        "Step 5: Verify the page redirects to the dashboard"
      ],
      "test_data": {
        "input": "email=validuser@example.com, password=SecurePass@123",
        "notes": "Use a pre-registered account with verified email"
      },
      "expected_result": "User is successfully authenticated and redirected to the dashboard.",
      "priority": "High",
      "type": "Functional",
      "negative": false,
      "automation_candidate": true
    },
    {
      "test_case_id": "TC_002",
      "title": "Login fails with incorrect password",
      "objective": "Verify that the system shows an error message when an incorrect password is entered.",
      "preconditions": [
        "User is registered with a known email",
        "User is on the login page"
      ],
      "test_steps": [
        "Step 1: Navigate to the login page",
        "Step 2: Enter a valid registered email address",
        "Step 3: Enter an incorrect password",
        "Step 4: Click the Login button"
      ],
      "test_data": {
        "input": "email=validuser@example.com, password=WrongPassword123",
        "notes": "Use correct email but intentionally wrong password"
      },
      "expected_result": "Error message displayed: 'Invalid email or password.' User stays on the login page.",
      "priority": "High",
      "type": "Negative",
      "negative": true,
      "automation_candidate": true
    }
  ]
}
```

---

## 📐 JSON Schema Explained

The schema file (`schema/testcase-schema.json`) defines the exact structure that the LLM output must match. It is also placed in `src/main/resources/schema/` so the `SchemaValidator` can load it from the classpath at runtime.

### Top-Level Required Fields

| Field | Type | Required | Description |
|---|---|---|---|
| `feature_name` | string | ✅ | Name of the feature being tested |
| `requirement_summary` | string | ✅ min 10 chars | Short one-line summary |
| `assumptions` | array of strings | ❌ optional | AI-generated assumptions |
| `test_scenarios` | array of objects | ✅ min 3 items | The generated test cases |

### Per Test Scenario Required Fields

| Field | Type | Constraint | Description |
|---|---|---|---|
| `test_case_id` | string | Pattern: `TC_001` | Unique sequential ID |
| `title` | string | min 5 chars | Short descriptive name |
| `objective` | string | min 10 chars | What is being verified |
| `preconditions` | array | min 1 item | Setup needed before execution |
| `test_steps` | array | **min 3 items** | Step-by-step actions |
| `test_data` | object | required: input + notes | Input values for the test |
| `expected_result` | string | min 10 chars | Clear expected outcome |
| `priority` | string | **enum: High/Medium/Low** | Business priority |
| `type` | string | **enum: Functional/Negative/Boundary/Validation/Error Handling** | Test category |
| `negative` | boolean | true or false | Whether it's a negative test |
| `automation_candidate` | boolean | true or false | Suitable for automation? |

---

## 🔄 Schema Validation Flow

```
LLM Returns JSON String
        │
        ▼
SchemaValidator.validate(jsonString)
        │
        ├─ Is it valid JSON?  ──── NO ──► Return FAILED (parse error)
        │
        ├─ Does it have required fields? ── NO ──► Return FAILED
        │
        ├─ Are priority/type valid enums? ── NO ──► Return FAILED
        │
        └─ Are test_steps >= 3 per scenario? ── NO ──► Return FAILED
                │
                ▼ (all pass)
        Return PASSED ✅
                │
                ▼
        ResponseParser → OutputNormalizer → OutputWriter
```

### Retry Logic

```
Attempt 1 → Validate → FAIL
                │
                └─► buildCorrectionPrompt(requirement, badJSON, errors)
                          │
Attempt 2 → Validate → FAIL
                │
                └─► buildCorrectionPrompt(requirement, badJSON, errors)
                          │
Attempt 3 → Validate → FAIL
                │
                └─► writeDebugOutput(rawResponse) → EXIT
```

---

## 🛡️ Guardrails Enforced

These rules are enforced **both in the prompt** and **by schema validation**:

| # | Guardrail | Enforced By |
|---|---|---|
| 1 | LLM output must be JSON only | System prompt + JSON mode |
| 2 | No Markdown fences (\`\`\`json) | `stripMarkdownFences()` in client |
| 3 | No conversational text | System prompt strict rule |
| 4 | Every scenario must have `expected_result` | JSON Schema required field |
| 5 | Every scenario must have ≥ 3 `test_steps` | JSON Schema minItems:3 |
| 6 | `priority` must be High / Medium / Low | JSON Schema enum |
| 7 | `type` must be one of 5 allowed values | JSON Schema enum |
| 8 | Minimum 3 test scenarios generated | JSON Schema minItems:3 |
| 9 | All `test_case_id` values must be unique | `OutputNormalizer` reassigns sequentially |
| 10 | Invalid enum values corrected | `OutputNormalizer` fixes or defaults |
| 11 | Invalid output is retried automatically | `Main.java` retry loop |
| 12 | Raw response stored if all retries fail | `OutputWriter.writeDebugOutput()` |

---

## 📊 Console Output

When the application runs successfully, you will see:

```
╔══════════════════════════════════════════════════════════╗
║       AI TEST CASE GENERATOR — Controlled LLM Output     ║
║       Java + OpenAI + JSON Schema Validation             ║
╚══════════════════════════════════════════════════════════╝

🔑 API Key loaded from .env file.
📋 Requirement File   : requirements/login_requirement.txt
[RequirementReader] Reading requirement from file: requirements/login_requirement.txt
[RequirementReader] Requirement normalized. Length: 1024 chars.

🔄 Attempt 1 of 3
[OpenAiClient] Calling OpenAI API with model: gpt-4o
[OpenAiClient] Content extracted. Length: 3842 chars.

🔍 Running Schema Validation...
[SchemaValidator] ✅ Validation PASSED — JSON is schema-compliant.

🔧 Normalizing test scenarios...
[OutputNormalizer] Normalized 10 test scenarios.
[OutputWriter] SUCCESS: Output files written successfully.
[OutputWriter] JSON    -> output/generated_testcases.json
[OutputWriter] CSV     -> output/generated_testcases.csv
[OutputWriter] Summary -> output/summary.txt

============================================================
   SUCCESS: TEST CASE GENERATION COMPLETE
============================================================
  Requirement File      : requirements/login_requirement.txt
  Feature Name          : User Login
  Validation Status     : PASSED 
------------------------------------------------------------
  Total Scenarios       : 10
  High Priority         : 5
  Medium Priority       : 4
  Low Priority          : 1
  Negative Scenarios    : 6
  Automation Candidates : 8
------------------------------------------------------------
  Output JSON           : output/generated_testcases.json
  Summary Report        : output/summary.txt
============================================================
```

---

## 🧪 Unit Tests

The project includes **36 unit tests** organized in 3 test classes:

### `PromptBuilderTest.java` — 10 Tests
**Purpose:** Ensures the AI is being asked the *right questions with the right rules.*
Because LLMs are highly sensitive to prompt wording, this class verifies that your Java application is injecting the strict safety constraints into the prompt before sending it.

**What it checks:**
- ✅ System prompt enforces JSON-only output and no-markdown rules.
- ✅ System prompt requires negative scenarios and lists all priority enum values.
- ✅ User prompt embeds requirement text, specifies scenario counts, and enforces enums.
- ✅ Correction prompt includes the failed response and error summary for the retry loop.

### `SchemaValidatorTest.java` — 14 Tests
**Purpose:** Ensures the raw data returned by the AI is mathematically and structurally flawless.
This class acts as the "Bouncer" for the application, verifying that the AI's response matches the exact structure required by our JSON Schema (Draft-07).

**What it checks:**
- ✅ Valid JSON passing and reporting "No errors".
- ✅ Mandatory fields presence (feature_name, requirement_summary, test_scenarios).
- ✅ Negative constraints (fails on < 3 scenarios, invalid enums like "Critical", or < 3 steps).
- ✅ Robustness against edge cases (malformed JSON, empty strings, null inputs).

### `GeneratorFlowTest.java` — 12 Tests
**Purpose:** Tests the end-to-end internal mechanics of parsing, validating, and sanitizing the AI's response.
This ensures that the "Data Sweeper" (OutputNormalizer) can handle successfully validated JSON and turn it into perfect Java Objects.

**What it checks:**
- ✅ Successful parsing of JSON into `TestCaseResponse` models.
- ✅ Normalization logic: Seqential ID reassignment (`TC_001`, `TC_002`), duplicate removal, and enum fixing.
- ✅ Defaulting behavior: Ensures hallucinated enum values are safely defaulted back to "Functional" or "Medium".
- ✅ Pad logic: Ensures test steps are padded to the minimum requirement if the AI falls short.

### Run All Tests

```bash
mvn clean test
```

### Run a Specific Test Class

```bash
mvn test -Dtest=SchemaValidatorTest
mvn test -Dtest=PromptBuilderTest
mvn test -Dtest=GeneratorFlowTest
```

---

## 🌟 Benefits for QA Teams and SDETs

| Benefit | Details |
|---|---|
| **Instant Coverage** | Generates 8–12 test cases per requirement in seconds |
| **No Missing Negatives** | AI is forced to include negative and edge cases |
| **Standardized Format** | Machine-readable JSON — import into Jira, Zephyr, or Xray |
| **Pipeline-Ready** | JSON output plugs directly into CI/CD quality gates |
| **Traceability** | Each scenario has a unique ID, title, objective, and preconditions |
| **Automation-Friendly** | `automation_candidate` flag filters what to automate |
| **Portfolio-Ready** | Clean, modular Java code ideal for interview demonstrations |

---

## 🤖 Future: Selenium Integration

The project includes a bonus module `SeleniumSkeletonGenerator.java` that converts test cases where `automation_candidate == true` into Selenium TestNG Java code skeletons.

**Run the Selenium Generator after test case generation:**

```java
SeleniumSkeletonGenerator gen = new SeleniumSkeletonGenerator();
String skeleton = gen.generateSkeleton(normalizedResponse);
FileUtils.writeFile("output/SeleniumTests.java", skeleton);
```

**Sample Generated Skeleton:**

```java
@Test(description = "Successful login with valid email and password",
      groups = {"functional", "smoke"})
public void successfulLoginWithValidEmailAndPassword() {
    // Preconditions:
    //   - User is registered and email is verified
    //   - User is on the login page

    // Step 1: Navigate to the login page URL
    // TODO: Implement - pageObject.navigateToTheLoginPageUrl();
    // Step 2: Enter a valid registered email address
    // TODO: Implement - pageObject.enterAValidRegisteredEmailAddress();
    // Step 3: Enter the correct password
    // TODO: Implement - pageObject.enterTheCorrectPassword();

    // Expected: User is successfully authenticated and redirected to the dashboard.
    // Assert.assertTrue(condition, "User is successfully authenticated...");
}
```

**Design Principle:** Every test step is written as an **actionable sentence** so it maps cleanly to a Page Object method name. This is done intentionally to make the Selenium integration seamless.

---

## 📁 Additional Notes

### Adding a New Requirement

1. Create a new `.txt` file in the `requirements/` folder:
   ```
   requirements/password_reset_requirement.txt
   ```

2. Write the requirement in plain English — no special format needed.

3. Run the generator pointing to your new file:
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.aitestgen.Main" \
     -Dexec.args="requirements/password_reset_requirement.txt"
   ```

---

### Changing the LLM Model

Open `OpenAiClient.java` and change the model constant:

```java
// Currently set to:
private static final String MODEL = "gpt-4o";

// To use GPT-4o-mini (faster, cheaper, slightly less accurate):
private static final String MODEL = "gpt-4o-mini";
```

---

### Adjusting Retry Count

Open `Main.java` and change:

```java
// Default: 2 retries (3 total attempts)
private static final int MAX_RETRIES = 2;
```

---

## 👨‍💻 Author

Built as a portfolio-ready, interview-quality project demonstrating:
- Controlled AI integration in a Java ecosystem
- JSON Schema validation for LLM output
- Modular SDET-friendly architecture
- Strong test coverage (36 unit tests)
- Clean separation of concerns

---

*AI Test Case Generator · Java · OpenAI · Maven · Jackson · OkHttp · networknt*
