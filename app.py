from flask import Flask, request, jsonify
from flask_cors import CORS
import google.generativeai as genai
import os
from dotenv import load_dotenv
import logging
import time
from datetime import datetime
import traceback

# Load environment variables
load_dotenv()

# Configure logging with more detailed format
logging.basicConfig(
    level=logging.DEBUG,  # Changed to DEBUG for more verbose logging
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s - [%(filename)s:%(lineno)d]',
    handlers=[
        logging.FileHandler("api.log"),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Initialize Flask app
app = Flask(__name__)
CORS(app)

# Get API key from environment variables
API_KEY = os.getenv("GEMINI_API_KEY")

# Validate API key
if not API_KEY:
    logger.error("GEMINI_API_KEY not found in environment variables")
    raise ValueError("GEMINI_API_KEY environment variable is required")

if API_KEY =="AIzaSyAxsi_rtC7cYHrrfpBS7F6tI2cnQrcAXRo":
    logger.warning("Using hardcoded API key - this should be moved to environment variables")

# Configure Gemini AI with better error handling
try:
    genai.configure(api_key=API_KEY)
    model = genai.GenerativeModel("gemini-1.5-pro")
    logger.info("Gemini AI model initialized successfully")
    
    # Test the connection
    test_response = model.generate_content("Hello, this is a test.")
    logger.info("Gemini AI connection test successful")
    
except Exception as e:
    logger.error(f"Failed to initialize Gemini AI: {str(e)}")
    logger.error(f"Full traceback: {traceback.format_exc()}")
    raise

@app.route("/", methods=["GET"])
def home():
    """Home page endpoint"""
    return """
    <html>
        <head><title>TherapyPal API</title></head>
        <body>
            <h1>Welcome to TherapyPal API!</h1>
            <p>The server is running.</p>
            <p>Use POST /chat with {"message": "your message"} to interact with the API.</p>
            <p>Check <a href="/health">health status</a></p>
            <div style="margin-top: 20px;">
                <h3>Test the API:</h3>
                <button onclick="testAPI()">Test Chat Endpoint</button>
                <div id="result" style="margin-top: 10px; padding: 10px; border: 1px solid #ccc;"></div>
            </div>
            <script>
                async function testAPI() {
                    const resultDiv = document.getElementById('result');
                    resultDiv.innerHTML = 'Testing...';
                    
                    try {
                        const response = await fetch('/chat', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                            },
                            body: JSON.stringify({
                                message: 'Hello, this is a test message!'
                            })
                        });
                        
                        const data = await response.json();
                        
                        if (response.ok) {
                            resultDiv.innerHTML = `<strong>Success:</strong><br>${data.response}`;
                            resultDiv.style.backgroundColor = '#d4edda';
                            resultDiv.style.color = '#155724';
                        } else {
                            resultDiv.innerHTML = `<strong>Error:</strong><br>${data.error}<br><strong>Details:</strong> ${data.details || 'N/A'}`;
                            resultDiv.style.backgroundColor = '#f8d7da';
                            resultDiv.style.color = '#721c24';
                        }
                    } catch (error) {
                        resultDiv.innerHTML = `<strong>Network Error:</strong><br>${error.message}`;
                        resultDiv.style.backgroundColor = '#f8d7da';
                        resultDiv.style.color = '#721c24';
                    }
                }
            </script>
        </body>
    </html>
    """

@app.route("/health", methods=["GET"])
def health_check():
    """Enhanced health check endpoint"""
    try:
        # Test Gemini connection
        test_response = model.generate_content("Test")
        gemini_status = "healthy"
    except Exception as e:
        logger.error(f"Gemini health check failed: {str(e)}")
        gemini_status = f"unhealthy: {str(e)}"
    
    return jsonify({
        "status": "healthy" if gemini_status == "healthy" else "degraded",
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0",
        "services": {
            "gemini": gemini_status,
            "api_key_configured": bool(API_KEY and len(API_KEY) > 10)
        }
    })

@app.route("/chat", methods=["POST"])
def chat():
    """Main endpoint for interacting with Gemini with enhanced error handling"""
    try:
        # Log the incoming request
        logger.info(f"Received chat request from {request.remote_addr}")
        
        # Get the request data
        data = request.json
        if not data:
            logger.warning("Request received with no JSON data")
            return jsonify({"error": "No JSON data provided"}), 400
            
        if "message" not in data:
            logger.warning("Request received without 'message' field")
            return jsonify({"error": "Missing 'message' field in request"}), 400
            
        user_input = data["message"]
        
        # Validate message
        if not user_input or not user_input.strip():
            return jsonify({"error": "Message cannot be empty"}), 400
            
        if len(user_input) > 10000:  # Reasonable limit
            return jsonify({"error": "Message too long (max 10000 characters)"}), 400
            
        logger.info(f"Processing message: {user_input[:100]}{'...' if len(user_input) > 100 else ''}")
        
        # Generate response with detailed retry mechanism
        max_retries = 3
        retry_count = 0
        last_error = None
        
        while retry_count < max_retries:
            try:
                logger.debug(f"Attempt {retry_count + 1} to generate content")
                
                # Add safety settings if needed
                safety_settings = [
                    {
                        "category": "HARM_CATEGORY_HARASSMENT",
                        "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                    },
                    {
                        "category": "HARM_CATEGORY_HATE_SPEECH",
                        "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                    }
                ]
                
                response = model.generate_content(
                    user_input,
                    safety_settings=safety_settings
                )
                
                # Check if response was blocked
                if not response.text:
                    if response.prompt_feedback:
                        logger.warning(f"Response blocked: {response.prompt_feedback}")
                        return jsonify({
                            "error": "Response was blocked due to safety filters",
                            "details": str(response.prompt_feedback)
                        }), 400
                    else:
                        logger.warning("Empty response received from Gemini")
                        return jsonify({
                            "error": "Empty response received",
                            "details": "The AI model returned an empty response"
                        }), 500
                
                logger.info(f"Successfully generated response of length {len(response.text)}")
                
                # Return the successful response
                return jsonify({
                    "response": response.text,
                    "timestamp": datetime.now().isoformat(),
                    "model": "gemini-1.5-pro"
                })
                
            except Exception as e:
                retry_count += 1
                last_error = e
                error_msg = str(e)
                
                logger.warning(f"Attempt {retry_count} failed: {error_msg}")
                logger.debug(f"Full error traceback: {traceback.format_exc()}")
                
                # Check for specific error types
                if "quota exceeded" in error_msg.lower():
                    logger.error("API quota exceeded")
                    return jsonify({
                        "error": "API quota exceeded",
                        "details": "The API quota has been exceeded. Please try again later."
                    }), 429
                
                if "invalid api key" in error_msg.lower() or "authentication" in error_msg.lower():
                    logger.error("API key authentication failed")
                    return jsonify({
                        "error": "Authentication failed",
                        "details": "Invalid API key or authentication error"
                    }), 401
                
                if retry_count < max_retries:
                    wait_time = retry_count * 2  # Exponential backoff
                    logger.info(f"Waiting {wait_time} seconds before retry...")
                    time.sleep(wait_time)
                else:
                    # Final attempt failed
                    logger.error(f"All {max_retries} attempts failed. Last error: {error_msg}")
                    raise last_error
                
    except Exception as e:
        error_msg = str(e)
        logger.error(f"Error processing chat request: {error_msg}")
        logger.error(f"Full traceback: {traceback.format_exc()}")
        
        return jsonify({
            "error": "Failed to generate response",
            "details": error_msg,
            "timestamp": datetime.now().isoformat()
        }), 500

# Handle 404 errors
@app.errorhandler(404)
def not_found(e):
    logger.warning(f"404 error: {request.url}")
    return jsonify({"error": "Endpoint not found"}), 404

# Handle 500 errors
@app.errorhandler(500)
def server_error(e):
    logger.error(f"500 error: {str(e)}")
    return jsonify({"error": "Internal server error"}), 500

if __name__ == "__main__":
    # Get port from environment variable or use default
    port = int(os.environ.get("PORT", 5000))
    
    # Set debug based on environment
    debug = os.environ.get("FLASK_ENV") == "development"
    
    logger.info(f"Starting server on port {port}")
    logger.info(f"Debug mode: {debug}")
    logger.info(f"API key configured: {bool(API_KEY and len(API_KEY) > 10)}")
    
    app.run(host='0.0.0.0', port=port, debug=debug)