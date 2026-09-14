ALTER TABLE assessment.attempt_response_save_requests
    ADD COLUMN saved_response_type_code VARCHAR(64),
    ADD COLUMN saved_response_payload JSONB;

UPDATE assessment.attempt_response_save_requests request
SET saved_response_type_code = response.question_type_code,
    saved_response_payload = response.response_payload
FROM assessment.attempt_item_responses response
WHERE response.attempt_id = request.attempt_id
  AND response.global_position = request.global_position;

ALTER TABLE assessment.attempt_response_save_requests
    ALTER COLUMN saved_response_type_code SET NOT NULL,
    ALTER COLUMN saved_response_payload SET NOT NULL,
    ADD CONSTRAINT attempt_response_save_requests_payload_object_check
        CHECK (jsonb_typeof(saved_response_payload) = 'object');
