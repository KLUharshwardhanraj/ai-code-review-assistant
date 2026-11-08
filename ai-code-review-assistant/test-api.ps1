# ==============================================================================
# Quick Automated API Test Script for AI Code Review Assistant
# ==============================================================================
$BaseUrl = "http://localhost:8080/api/v1/reviews"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   AI Code Review Assistant - Automated REST API Verification" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Check Health Endpoint
Write-Host "[1/4] Checking Actuator Health Endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
    Write-Host " -> Health Status: $($health.status)" -ForegroundColor Green
    Write-Host " -> Gemini Provider: $($health.components.geminiApi.details.provider) ($($health.components.geminiApi.details.model))" -ForegroundColor Gray
} catch {
    Write-Host " -> Failed to connect to backend on http://localhost:8080. Is the backend running?" -ForegroundColor Red
    exit 1
}

# 2. Submit Code Snippet
Write-Host "`n[2/4] Submitting Java Code Snippet for AI Review..." -ForegroundColor Yellow
$SubmitBody = @{
    code = @"
public class OrderService {
    public void process(Order order) {
        // Potential NullPointerException:
        order.getItems().clear();
    }
}
"@
    language = "java"
    submission_type = "SNIPPET"
    title = "OrderService Null Check Review"
} | ConvertTo-Json

try {
    $session = Invoke-RestMethod -Uri "$BaseUrl/submit" -Method Post -Body $SubmitBody -ContentType "application/json"
    $SessionId = $session.session_id
    Write-Host " -> Session Created Successfully! ID = $SessionId" -ForegroundColor Green
    Write-Host " -> Status: $($session.status)" -ForegroundColor Gray
    Write-Host " -> Gemini Duration: $($session.gemini_request_duration_ms) ms" -ForegroundColor Gray
    Write-Host " -> Total Findings: $($session.total_count)" -ForegroundColor Gray

    # Print feedback items
    foreach ($item in $session.feedbacks) {
        Write-Host "    * [Line $($item.line_number)] [$($item.category)] [$($item.severity)] $($item.suggestion)" -ForegroundColor White
    }
} catch {
    Write-Host " -> Submission failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# 3. Update Decision (Human-in-the-Loop Accept)
if ($session.feedbacks.Count -gt 0) {
    $FirstFeedbackId = $session.feedbacks[0].id
    Write-Host "`n[3/4] Updating Decision for Feedback ID #$FirstFeedbackId to 'ACCEPTED'..." -ForegroundColor Yellow
    
    $DecisionBody = @{ status = "ACCEPTED" } | ConvertTo-Json
    try {
        $updated = Invoke-RestMethod -Uri "$BaseUrl/feedback/$FirstFeedbackId/decision" -Method Patch -Body $DecisionBody -ContentType "application/json"
        Write-Host " -> Feedback Decision Updated to: $($updated.status)" -ForegroundColor Green
    } catch {
        Write-Host " -> Decision update failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 4. Check Updated Session Details & Metrics
Write-Host "`n[4/4] Fetching Full Session Details by ID #$SessionId..." -ForegroundColor Yellow
try {
    $latestSession = Invoke-RestMethod -Uri "$BaseUrl/sessions/$SessionId" -Method Get
    Write-Host " -> Session Status: $($latestSession.status)" -ForegroundColor Green
    Write-Host " -> Accepted Count: $($latestSession.accepted_count) / $($latestSession.total_count)" -ForegroundColor Gray
    Write-Host " -> Rejected Count: $($latestSession.rejected_count)" -ForegroundColor Gray
    Write-Host " -> Pending Count:  $($latestSession.pending_count)" -ForegroundColor Gray
} catch {
    Write-Host " -> Fetch failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "   API Verification Completed Successfully!" -ForegroundColor Green
Write-Host "   Frontend is available at: http://localhost:5173" -ForegroundColor Cyan
Write-Host "   Swagger UI is available at: http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
