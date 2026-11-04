# Edge AI Model Deployment Platform — Hardware Compatibility Check V1

## 1. Objective

Implement **Hardware Compatibility Check V1** for the Edge AI Model Deployment Platform.

The current platform already supports:

```text
React
  ↓
POST /models
  ↓
Spring Boot
  ↓
Upload model
  ↓
S3
  ↓
Analysis Job
  ↓
Python Model Worker
  ↓
onnx.load()
  ↓
Model Analysis JSON
  ↓
Spring Boot
  ↓
React
```

The next feature is to determine whether an analyzed ONNX model is compatible with a selected target hardware profile.

### V1 scope

Support only:

- Hardware: NVIDIA Jetson Orin Nano Super Developer Kit
- Model format: ONNX
- Compatibility result: `COMPATIBLE`, `INCOMPATIBLE`, or `WARNING`
- Operator compatibility
- Basic data type / precision compatibility
- Basic model metadata checks
- Persistent compatibility results
- REST APIs
- React UI

### Out of scope for V1

Do not implement:

- TensorRT engine building
- Real Jetson hardware execution
- Performance benchmarking
- Automatic ONNX model modification
- Automatic operator replacement
- Multi-hardware support
- User authentication changes
- Billing/subscriptions
- AI/LLM-based recommendations

The goal is to build a clean foundation that can later support TensorRT build and real hardware validation.

---

## 2. Product Goal

A customer uploads an ONNX model and selects a target hardware device.

The platform should answer:

> Can this model potentially run on this target hardware, based on the model's analyzed characteristics and the hardware compatibility profile?

Example:

```text
Model:
defect-detector.onnx

Target:
NVIDIA Jetson Orin Nano Super

Result:
COMPATIBLE
```

If there are problems:

```text
Result:
INCOMPATIBLE

Unsupported Operators:
- CustomXYZ

Warnings:
- Dynamic input shape detected
```

The system must explain why a model is considered incompatible or potentially problematic.

---

## 3. Current Model Analysis Output

The existing Python Model Worker already produces Model Analysis JSON.

The compatibility feature must consume this existing analysis result rather than re-parsing the ONNX file unnecessarily.

Example:

```json
{
  "model": {
    "format": "ONNX",
    "opset": 17,
    "fileSize": 45200000,
    "parameters": 11689512
  },
  "inputs": [
    {
      "name": "input",
      "shape": [1, 3, 224, 224],
      "dtype": "float32"
    }
  ],
  "outputs": [
    {
      "name": "output",
      "shape": [1, 2],
      "dtype": "float32"
    }
  ],
  "operators": {
    "Conv": 20,
    "Relu": 17,
    "MaxPool": 5,
    "Gemm": 1,
    "Softmax": 1
  }
}
```

Do not change the existing Model Analysis implementation unless required for clean integration.

---

## 4. Hardware Profile

Create a hardware profile abstraction.

The frontend must not hard-code hardware compatibility information.

The backend should expose hardware profiles through an API.

### Hardware Profile V1

Initial hardware:

```text
Vendor:
NVIDIA

Name:
Jetson Orin Nano Super Developer Kit

Architecture:
NVIDIA Ampere

Runtime:
TensorRT

Supported Precision:
FP32
FP16
INT8
```

Important:

The exact supported operator list and runtime constraints should be represented as configurable data, not scattered through application code.

For V1, use a clearly documented initial compatibility profile.

Structure the implementation so that future hardware can be added without rewriting the compatibility engine.

---

## 5. Recommended Data Model

Create a `hardware_profiles` table/entity.

Suggested fields:

```text
id
vendor
name
architecture
runtime
runtimeVersion (optional)
supportedPrecisions
supportedOperators
description
createdAt
updatedAt
```

Use the project's existing database conventions and technology.

Do not introduce a new database technology.

If the current project uses PostgreSQL, use PostgreSQL.

---

## 6. Compatibility Result Data Model

Create a persistent compatibility result.

Suggested fields:

```text
id
modelId
hardwareProfileId
status
unsupportedOperators
warnings
supportedPrecisions
analysisVersion (optional)
createdAt
```

Suggested status enum:

```text
COMPATIBLE
INCOMPATIBLE
WARNING
```

The result should preserve enough information to reproduce what the platform concluded at the time of the check.

If the project already has a generic Job entity, reuse it where appropriate.

---

## 7. Compatibility Rules V1

The compatibility engine should be deterministic.

Do not use an LLM.

Do not make vague AI-generated decisions.

The engine should compare:

```text
Model Analysis
       +
Hardware Profile
       ↓
Compatibility Rules
       ↓
Compatibility Result
```

### Rule 1 — Model format

The model must be ONNX.

If the model format is not ONNX:

```text
INCOMPATIBLE
```

For the current API this should normally never occur because V1 accepts ONNX models only.

### Rule 2 — Operators

For every operator in:

```text
analysis.operators
```

check whether the operator exists in the target hardware profile's supported operator set.

Example:

```text
Model:

Conv
Relu
MaxPool
Gemm
Softmax
```

Hardware:

```text
Conv       ✓
Relu       ✓
MaxPool    ✓
Gemm       ✓
Softmax    ✓
```

Result:

```text
COMPATIBLE
```

If the model contains:

```text
CustomXYZ
```

and the hardware profile does not support it:

```text
INCOMPATIBLE
```

The result should include:

```json
{
  "operator": "CustomXYZ",
  "count": 1,
  "reason": "Unsupported operator"
}
```

If an unsupported operator appears multiple times, preserve its count.

### Rule 3 — Precision / Data Type

Inspect the input and output data types from Model Analysis.

For example:

```text
float32
float16
int8
```

The compatibility result should identify whether the target profile supports the relevant precision.

For V1:

```text
FP32 → supported
FP16 → supported
INT8 → supported
```

Do not claim that INT8 model quantization is supported merely because the hardware supports INT8.

Hardware precision support and model quantization are different concepts.

### Rule 4 — Shape

Inspect model input shapes.

Example:

```text
[1, 3, 224, 224]
```

This is a static shape.

If a dimension is dynamic, such as:

```text
[-1, 3, 224, 224]
```

or an ONNX symbolic dimension such as:

```text
["batch", 3, 224, 224]
```

do not automatically mark the model incompatible.

Instead:

```text
WARNING
```

Example:

```text
Dynamic input shape detected.
Additional runtime/build configuration may be required.
```

### Rule 5 — Opset

Inspect the ONNX opset from the model analysis.

The hardware profile may define a supported opset range:

```text
minimumOpset
maximumOpset
```

If the model's opset is outside the configured range:

```text
INCOMPATIBLE
```

If no opset constraint is configured, do not invent one.

---

## 8. Result Decision Logic

Use deterministic precedence:

```text
If a blocking incompatibility exists:
    INCOMPATIBLE

Else if one or more warnings exist:
    WARNING

Else:
    COMPATIBLE
```

Examples:

```text
Unsupported operator
+
Dynamic input shape

Result:
INCOMPATIBLE
```

```text
All operators supported
+
Dynamic input shape

Result:
WARNING
```

```text
All checks pass

Result:
COMPATIBLE
```

---

## 9. Backend API

### Get Hardware Profiles

```http
GET /api/hardware
```

Example response:

```json
[
  {
    "id": "jetson-orin-nano-super",
    "vendor": "NVIDIA",
    "name": "Jetson Orin Nano Super Developer Kit",
    "architecture": "NVIDIA Ampere",
    "runtime": "TensorRT",
    "supportedPrecisions": [
      "FP32",
      "FP16",
      "INT8"
    ]
  }
]
```

### Get Hardware Profile

```http
GET /api/hardware/{hardwareId}
```

### Run Compatibility Check

```http
POST /api/models/{modelId}/compatibility-check
```

Request:

```json
{
  "hardwareId": "jetson-orin-nano-super"
}
```

Response:

```json
{
  "id": "compatibility-result-id",
  "modelId": "model-id",
  "hardwareId": "jetson-orin-nano-super",
  "status": "COMPATIBLE",
  "unsupportedOperators": [],
  "warnings": [],
  "supportedPrecisions": [
    "FP32",
    "FP16",
    "INT8"
  ]
}
```

### Get Compatibility Result

```http
GET /api/models/{modelId}/compatibility
```

Return the most recent compatibility result, or follow the project's existing job/result conventions.

---

## 10. Job Handling

Compatibility checking should be implemented as a job if the existing application already uses asynchronous jobs.

Recommended flow:

```text
POST /compatibility-check
        ↓
Create Job
        ↓
QUEUED
        ↓
CHECKING
        ↓
COMPLETED
```

On failure:

```text
FAILED
```

If the compatibility calculation is currently fast enough to be synchronous, keep the implementation simple, but preserve a clean path for asynchronous execution later.

---

## 11. Frontend UI

Add hardware selection to the Model Detail / Deployment workflow.

Example:

```text
Model:
defect-detector.onnx

Target Hardware

[ NVIDIA Jetson Orin Nano Super ▼ ]

[ Check Compatibility ]
```

After checking:

```text
┌──────────────────────────────────────┐
│ Compatibility Result                 │
│                                      │
│              ✓                       │
│         COMPATIBLE                   │
│                                      │
│ Target                               │
│ Jetson Orin Nano Super               │
│                                      │
│ Operators                            │
│ Conv                 ✓               │
│ Relu                 ✓               │
│ MaxPool              ✓               │
│ Gemm                 ✓               │
│ Softmax              ✓               │
│                                      │
│ Precision                            │
│ FP32                 ✓               │
│ FP16                 ✓               │
│ INT8                 ✓               │
│                                      │
│ [ Build for Hardware ]              │
└──────────────────────────────────────┘
```

For incompatible models:

```text
┌──────────────────────────────────────┐
│ Compatibility Result                 │
│                                      │
│              ✕                       │
│        INCOMPATIBLE                  │
│                                      │
│ Unsupported Operators                │
│                                      │
│ CustomXYZ             ✕              │
│ Count: 1                             │
│                                      │
│ Reason:                              │
│ Target runtime does not support      │
│ this operator.                       │
└──────────────────────────────────────┘
```

For warnings:

```text
WARNING

Dynamic input shape detected.

The model may require additional
runtime/build configuration.
```

---

## 12. User Experience

The workflow should be:

```text
1. User uploads ONNX model

2. Model Analysis completes

3. User opens Model Detail

4. User selects:
   NVIDIA Jetson Orin Nano Super

5. User clicks:
   Check Compatibility

6. Platform analyzes the model

7. Platform displays:
   COMPATIBLE / WARNING / INCOMPATIBLE

8. User can inspect:
   - unsupported operators
   - warnings
   - precision information
   - model information

9. If compatible:
   show a clear next-step CTA:

   "Build for this Hardware"
```

The Build button should be disabled or hidden when the model is clearly incompatible.

---

## 13. Error Handling

Handle at minimum:

```text
Model not found
Hardware profile not found
Model analysis not available
Invalid analysis data
Unsupported model format
Compatibility engine error
Database error
```

Return appropriate HTTP status codes.

Do not expose stack traces or internal exceptions to the frontend.

Use the project's existing error response format if one exists.

---

## 14. Security

The compatibility endpoint must verify that the requested model belongs to the current user/account if authentication/authorization already exists.

Do not allow a user to:

- Read another user's model
- Run checks against another user's model
- Access another user's analysis result

Do not trust `modelId` or `hardwareId` from the client without backend validation.

---

## 15. Architecture Requirements

Keep responsibilities separated.

### React

Responsible for:

```text
UI
API calls
Loading states
Error states
Result visualization
```

### Spring Boot

Responsible for:

```text
REST API
Authorization
Model ownership
Hardware profiles
Compatibility result persistence
Job management
Business orchestration
```

### Python Model Worker

Responsible for:

```text
Model Analysis
Compatibility calculation if that matches
the existing worker architecture
```

Do not duplicate ONNX parsing logic in Java and Python.

Reuse the existing Model Analysis JSON.

---

## 16. Testing Requirements

Implement unit and integration tests.

Minimum test cases:

### Test 1 — Fully compatible model

```text
All operators supported
Static shape
Supported precision
Valid opset

Expected:
COMPATIBLE
```

### Test 2 — Unsupported operator

```text
Model contains CustomXYZ
Hardware does not support it

Expected:
INCOMPATIBLE
```

### Test 3 — Dynamic shape

```text
All operators supported
Dynamic input shape

Expected:
WARNING
```

### Test 4 — Unsupported precision

```text
Model requires unsupported precision

Expected:
INCOMPATIBLE
```

### Test 5 — Multiple unsupported operators

Verify that all unsupported operators are returned, with correct counts.

### Test 6 — Missing model

Expected:

```text
404
```

### Test 7 — Missing hardware profile

Expected:

```text
404
```

### Test 8 — Unauthorized model access

Expected:

```text
403
```

or the project's standard authorization response.

---

## 17. Definition of Done

This feature is complete when:

- [ ] Hardware profiles can be stored in the database.
- [ ] `GET /api/hardware` works.
- [ ] `GET /api/hardware/{hardwareId}` works.
- [ ] User can select Jetson Orin Nano Super in React.
- [ ] User can start a compatibility check.
- [ ] Backend loads the existing Model Analysis result.
- [ ] Compatibility engine checks operators.
- [ ] Compatibility engine checks relevant data types/precision.
- [ ] Compatibility engine detects dynamic shapes and generates warnings.
- [ ] Compatibility engine checks configured opset constraints.
- [ ] Result is persisted.
- [ ] Result can be retrieved through an API.
- [ ] React displays COMPATIBLE / WARNING / INCOMPATIBLE.
- [ ] Unsupported operators and warnings are displayed.
- [ ] Unit tests cover the core compatibility rules.
- [ ] Integration tests cover the main API flow.
- [ ] No TensorRT build is implemented yet.
- [ ] No real Jetson hardware execution is implemented yet.
- [ ] Existing Model Analysis functionality continues to work.

---

## 18. Future Architecture

Do not implement these features now, but keep the design extensible for:

```text
Model
  ↓
Model Analysis
  ↓
Hardware Selection
  ↓
Compatibility Check
  ↓
Build
  ↓
Hardware Validation
  ↓
Benchmark
  ↓
Deployment Package
```

Eventually:

```text
                    Model
                      │
                      ▼
               Model Analysis
                      │
                      ▼
             Hardware Selection
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
       Jetson       Hailo      Qualcomm
          │           │           │
          ▼           ▼           ▼
     Compatibility Compatibility Compatibility
          │           │           │
          ▼           ▼           ▼
        Build        Build        Build
          │           │           │
          ▼           ▼           ▼
       Hardware    Hardware    Hardware
        Test         Test        Test
          │           │           │
          └───────────┼───────────┘
                      ▼
                  Benchmark
                      │
                      ▼
              Deployment Package
```

The current implementation should therefore be treated as:

> **Compatibility Engine V1 — ONNX + Jetson Orin Nano Super**

not as the final multi-NPU compatibility system.

---

## 19. Coding Instructions for the AI Coding Agent

Before modifying code:

1. Inspect the existing repository structure.
2. Read `AGENTS.md`, if present.
3. Inspect the existing Model Analysis implementation.
4. Inspect existing database entities/migrations.
5. Inspect existing REST controller/service patterns.
6. Inspect existing React component and API patterns.
7. Inspect existing job architecture.
8. Reuse existing conventions instead of introducing parallel architectures.

Then implement the feature incrementally:

```text
Step 1:
Hardware profile model/entity

Step 2:
Database migration/seed data

Step 3:
Hardware profile repository/service

Step 4:
Hardware REST APIs

Step 5:
Compatibility domain model

Step 6:
Compatibility engine

Step 7:
Compatibility persistence

Step 8:
Compatibility REST API

Step 9:
React hardware selection UI

Step 10:
React compatibility result UI

Step 11:
Unit tests

Step 12:
Integration tests

Step 13:
Run the complete existing test suite

Step 14:
Create a clean Git branch

Step 15:
Commit changes

Step 16:
Create a pull request
```

Do not rewrite unrelated parts of the application.

Keep the implementation small, testable, and extensible.

---

## 20. Expected V1 User Flow

The final V1 experience should be:

```text
Upload Model
     ↓
Model Analysis
     ↓
Model Detail
     ↓
Select Target Hardware
     ↓
Jetson Orin Nano Super
     ↓
Check Compatibility
     ↓
┌─────────────────────────────┐
│ COMPATIBLE                  │
│                             │
│ Operators:      100% ✓      │
│ Precision:      Supported ✓ │
│ Shape:          Static ✓    │
│ Opset:          Supported ✓ │
│                             │
│ [ Build for Hardware ]      │
└─────────────────────────────┘
```

Or:

```text
┌─────────────────────────────┐
│ INCOMPATIBLE                │
│                             │
│ Unsupported Operators:      │
│                             │
│ CustomXYZ       2 ✕         │
│                             │
│ Reason:                     │
│ Target runtime does not     │
│ support this operator.      │
│                             │
│ [ View Details ]            │
└─────────────────────────────┘
```
