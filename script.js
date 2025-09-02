const chatBox = document.getElementById("chat-box");
const input = document.getElementById("user-input");
const sendBtn = document.getElementById("send-btn");
const newChatBtn = document.getElementById("new-chat-btn");

// Add welcome message when page loads
document.addEventListener("DOMContentLoaded", () => {
  setTimeout(() => {
    addMessage("Hi there! I'm TherapyPal. How are you feeling today?", "bot");
  }, 500);
});

// Handle sending messages
function handleSend() {
  const msg = input.value.trim();
  if (msg) {
    addMessage(msg, "user");
    input.value = "";
    input.focus();
    
    // Show typing indicator
    showTypingIndicator();
    
    // Call the function to send to Flask backend
    sendToBackend(msg);
  }
}

// Handle new chat
newChatBtn.addEventListener("click", () => {
  chatBox.innerHTML = "";
  setTimeout(() => {
    addMessage("Starting a new conversation. How can I help you today?", "bot");
  }, 300);
});

// Event listeners
sendBtn.addEventListener("click", handleSend);
input.addEventListener("keypress", (e) => {
  if (e.key === "Enter") {
    handleSend();
  }
});

// Focus input when page loads
window.onload = () => {
  input.focus();
};

// Show typing indicator
function showTypingIndicator() {
  const typingDiv = document.createElement("div");
  typingDiv.className = "message bot typing";
  typingDiv.innerHTML = "<span class='dot'></span><span class='dot'></span><span class='dot'></span>";
  typingDiv.id = "typing-indicator";
  chatBox.appendChild(typingDiv);
  chatBox.scrollTop = chatBox.scrollHeight;
}

// Remove typing indicator
function removeTypingIndicator() {
  const typingIndicator = document.getElementById("typing-indicator");
  if (typingIndicator) {
    typingIndicator.remove();
  }
}

// Add message to chat box
function addMessage(text, type) {
  // Remove typing indicator if it exists
  if (type === "bot") {
    removeTypingIndicator();
  }

  const msgDiv = document.createElement("div");
  msgDiv.className = `message ${type}`;
  msgDiv.textContent = text;
  chatBox.appendChild(msgDiv);
  chatBox.scrollTop = chatBox.scrollHeight;
}

// Send message to Flask backend
async function sendToBackend(userInput) {
  try {
    const response = await fetch("http://127.0.0.1:5000/chat", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ message: userInput })
    });

    const data = await response.json();
    const botReply = data.response || data.error || "No response from AI.";
    
    // Add slight delay to simulate thinking
    setTimeout(() => {
      addMessage(botReply, "bot");
    }, 500 + Math.random() * 800);
  } catch (error) {
    console.error("Error sending message:", error);
    setTimeout(() => {
      addMessage("Sorry, I'm having trouble connecting. Please try again later.", "bot");
    }, 500);
  }
}