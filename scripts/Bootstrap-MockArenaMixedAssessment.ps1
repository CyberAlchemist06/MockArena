[CmdletBinding()]
param(
  [string]$QuestionBaseUrl = 'http://localhost:8080',
  [string]$ChallengeBaseUrl = 'http://localhost:8081',
  [string]$AssessmentBaseUrl = 'http://localhost:8083',
  [string]$IdentityBaseUrl = 'http://localhost:8082',
  [string]$IdentityEmail,
  [securestring]$IdentityPassword
)

$ErrorActionPreference = 'Stop'
$marker = 'mockarena-bootstrap-mixed-v1'
$statePath = Join-Path $PSScriptRoot '..\.local\mockarena-mixed-assessment.json'
if ($env:MOCKARENA_DEVELOPMENT_BOOTSTRAP -ne 'true') { throw 'Refusing bootstrap: set MOCKARENA_DEVELOPMENT_BOOTSTRAP=true and run only against explicitly configured local dev services.' }
foreach ($url in @($IdentityBaseUrl, $QuestionBaseUrl, $ChallengeBaseUrl, $AssessmentBaseUrl)) { try { Invoke-RestMethod "$url/actuator/health" | Out-Null } catch { throw "Required local service is unavailable: $url" } }
if ([string]::IsNullOrWhiteSpace($IdentityEmail)) { $IdentityEmail = Read-Host 'Local Identity email' }
if ($null -eq $IdentityPassword) { $IdentityPassword = Read-Host 'Local Identity password' -AsSecureString }
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($IdentityPassword)
try { $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer); $login = Invoke-RestMethod -Method POST -Uri "$IdentityBaseUrl/api/v1/auth/login" -ContentType 'application/json' -Body (@{email=$IdentityEmail;password=$plainPassword} | ConvertTo-Json -Compress) -ErrorAction Stop }
catch { throw 'Identity authentication failed. Start Identity in its documented dev mode, bootstrap/register a local user, and supply valid credentials.' }
finally { if ($passwordPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer) }; Remove-Variable plainPassword -ErrorAction SilentlyContinue }
if ([string]::IsNullOrWhiteSpace($login.accessToken)) { throw 'Identity login did not return an access token.' }
$authHeaders = @{ Authorization = "Bearer $($login.accessToken)" }
if (-not (Test-Path (Split-Path $statePath))) { New-Item -ItemType Directory -Path (Split-Path $statePath) -Force | Out-Null }

function Invoke-Json($method, $url, $body, [switch]$Anonymous) { $params = @{ Method=$method; Uri=$url; ContentType='application/json'; ErrorAction='Stop' }; if (-not $Anonymous) { $params.Headers = $authHeaders }; if ($null -ne $body) { $params.Body = ($body | ConvertTo-Json -Depth 12 -Compress) }; Invoke-RestMethod @params }
function Resolve-Questions { Invoke-Json POST "$QuestionBaseUrl/internal/v2/question-versions/resolve" @{ taxonomyAll=@(@{scheme='topic';code=$marker}); questionTypeCodes=@('MCQ','CODING'); difficultyProfiles=@(); contentLocales=@(); programmingLanguages=@(); limit=100 } -Anonymous }

$resolved = Resolve-Questions
if ($resolved.entries.Count -eq 0) {
  $questions = @(
    @{title='SE Bootstrap MCQ: HTTP method'; type='MCQ'; prompt='Which HTTP method is normally used to retrieve a resource?'; options=@(@{id='get';text='GET'},@{id='post';text='POST'}); correct='get'},
    @{title='SE Bootstrap MCQ: SQL join'; type='MCQ'; prompt='Which join returns rows with matching values in both tables?'; options=@(@{id='inner';text='INNER JOIN'},@{id='left';text='LEFT JOIN'}); correct='inner'},
    @{title='SE Bootstrap MCQ: immutability'; type='MCQ'; prompt='Which property prevents an object from changing after construction?'; options=@(@{id='immutable';text='Immutability'},@{id='inheritance';text='Inheritance'}); correct='immutable'},
    @{title='SE Bootstrap MCQ: queue'; type='MCQ'; prompt='Which ordering does a queue use?'; options=@(@{id='fifo';text='FIFO'},@{id='lifo';text='LIFO'}); correct='fifo'},
    @{title='SE Bootstrap MCQ: testing'; type='MCQ'; prompt='Which test checks a small unit in isolation?'; options=@(@{id='unit';text='Unit test'},@{id='load';text='Load test'}); correct='unit'},
    @{title='SE Bootstrap MCQ: version control'; type='MCQ'; prompt='Which operation records a local Git snapshot?'; options=@(@{id='commit';text='Commit'},@{id='clone';text='Clone'}); correct='commit'},
    @{title='SE Bootstrap CODING: Sum values'; type='CODING'; prompt='Return the sum of all integers in a list.'},
    @{title='SE Bootstrap CODING: Reverse text'; type='CODING'; prompt='Return the input text in reverse order.'},
    @{title='SE Bootstrap CODING: Count vowels'; type='CODING'; prompt='Count vowels in a text value.'},
    @{title='SE Bootstrap CODING: Maximum value'; type='CODING'; prompt='Return the greatest integer in a non-empty list.'}
  )
  foreach ($q in $questions) {
    if ($q.type -eq 'MCQ') { $content=@{title=$q.title;tags=@($marker);difficulty='EASY';questionType='MCQ';prompt=$q.prompt;options=$q.options;correctOptionId=$q.correct;explanation='See the relevant software engineering concept.'} }
    else { $content=@{title=$q.title;tags=@($marker);difficulty='EASY';questionType='CODING';prompt=$q.prompt;constraintsText='Use a clear, deterministic implementation.';examples=@(@{input='sample';output='sample'});supportedLanguages=@('JAVA');visibleTests=@(@{input='sample';output='sample'});hiddenTests=@(@{input='hidden';output='hidden'});scoringRules=@{maxPoints=100;strategy='ALL_OR_NOTHING'};executionLimits=@{timeMs=1000;memoryMb=256}} }
    $created = Invoke-Json POST "$QuestionBaseUrl/api/v1/questions" @{content=$content}
    Invoke-Json POST "$QuestionBaseUrl/api/v1/questions/$($created.questionId)/versions/$($created.versionNumber)/publish" @{expectedQuestionVersion=0;expectedVersion=0} | Out-Null
  }
  $resolved = Resolve-Questions
}
if ($resolved.entries.Count -ne 10) { throw "Question bootstrap is not converged: expected exactly 10 '$marker' published versions, found $($resolved.entries.Count). Reset the local bootstrap data before retrying." }
$mcq = @($resolved.entries | Where-Object questionTypeCode -eq 'MCQ'); $coding = @($resolved.entries | Where-Object questionTypeCode -eq 'CODING')
if ($mcq.Count -ne 6 -or $coding.Count -ne 4 -or (@($resolved.entries.questionId | Select-Object -Unique).Count -ne 10)) { throw 'Question bootstrap does not have exactly six unique MCQ and four unique CODING questions.' }

if (Test-Path $statePath) { $state = Get-Content -Raw $statePath | ConvertFrom-Json } else {
  $challenge = Invoke-Json POST "$ChallengeBaseUrl/api/v1/challenges" @{title='MockArena Software Engineer Mixed Challenge';visibility='PUBLIC';selectionGroups=@(
      @{taxonomyAll=@(@{scheme='topic';code=$marker});questionTypeCodes=@('MCQ');difficultyProfiles=@();contentLocales=@();programmingLanguages=@();requestedQuestionCount=6},
      @{taxonomyAll=@(@{scheme='topic';code=$marker});questionTypeCodes=@('CODING');difficultyProfiles=@();contentLocales=@();programmingLanguages=@('JAVA');requestedQuestionCount=4}
    )}
  if (@($challenge.resolvedQuestions).Count -ne 10) { throw 'Challenge did not resolve ten questions; refusing publication.' }
  $publishedChallenge = Invoke-Json POST "$ChallengeBaseUrl/api/v1/challenges/$($challenge.challengeId)/versions/1/publish" @{expectedChallengeVersion=0;expectedVersion=0}
  $assessment = Invoke-Json POST "$AssessmentBaseUrl/api/v1/assessments" @{visibility='PUBLIC';content=@{title='MockArena Software Engineer Assessment';description='A local mixed MCQ and coding development assessment.';instructions='Complete all questions.';assessmentTypeCode='STANDARD';timingPolicy=@{policyCode='FIXED_DURATION';parameters=@{}};attemptDurationSeconds=3600;attemptPolicy=@{policyCode='MAX_ATTEMPTS';parameters=@{maxAttempts=1}};resultReleasePolicy=@{policyCode='IMMEDIATE';parameters=@{}};challengeVersionIds=@($publishedChallenge.challengeVersionId)}}
  $publishedAssessment = Invoke-Json POST "$AssessmentBaseUrl/api/v1/assessments/$($assessment.assessmentId)/versions/1/publish" @{expectedAssessmentVersion=0;expectedVersion=0}
  $state=@{assessmentId=$publishedAssessment.assessmentId;assessmentVersionId=$publishedAssessment.assessmentVersionId;challengeId=$publishedChallenge.challengeId;challengeVersionId=$publishedChallenge.challengeVersionId}; $state | ConvertTo-Json | Set-Content $statePath
}
$detail = Invoke-RestMethod "$AssessmentBaseUrl/api/v1/public/assessments/$($state.assessmentId)"
if ($detail.questionCount -ne 10 -or $detail.questionTypeCounts.MCQ -ne 6 -or $detail.questionTypeCounts.CODING -ne 4) { throw 'Published catalogue projection does not report 10 / 6 / 4.' }
Write-Host "Mixed development assessment ready: assessment=$($state.assessmentId) assessmentVersion=$($state.assessmentVersionId) challenge=$($state.challengeId) challengeVersion=$($state.challengeVersionId) MCQ=6 CODING=4"
