import os
import google.generativeai as genai
from deep_translator import GoogleTranslator
from langdetect import detect, LangDetectException
import time
import random
from typing import Optional, List, Dict

# Configure the API
genai.configure(api_key="your_API")
print("API key set successfully!")

class TherapyPal:
    def __init__(self):
        self.model = genai.GenerativeModel("gemini-1.5-pro-latest")
        self.conversation_history = []
        self.user_language = "en"  # default to English
        self.setup_initial_prompt()
        self.setup_language_support()
        self.user_name = None
        self.session_count = 0
        self.emotion_patterns = self.load_emotion_patterns()
        self.personal_quirks = [
            "taking a moment to reflect",
            "pondering thoughtfully",
            "gently considering your words",
            "listening with complete presence"
        ]
        
    def setup_language_support(self):
        """Set up language codes and names for supported languages"""
        # Add language codes for Indian languages
        self.indian_language_codes = {
            "hi": "Hindi",
            "bn": "Bengali",
            "te": "Telugu",
            "ta": "Tamil",
            "mr": "Marathi",
            "gu": "Gujarati",
            "kn": "Kannada",
            "ml": "Malayalam",
            "pa": "Punjabi",
            "or": "Odia",
            "as": "Assamese",
            "sa": "Sanskrit",
            "ur": "Urdu"
        }
        
        # Add universal empathetic phrases
        self.empathetic_phrases = {
            "en": ["I understand", "I hear you", "That sounds difficult", "I'm here with you", 
                  "You're not alone in this", "It takes courage to share", "Your feelings are valid"],
            "hi": ["मैं समझता हूं", "मैं आपकी बात सुन रहा हूं", "यह मुश्किल लगता है", "मैं आपके साथ हूं", 
                  "आप इसमें अकेले नहीं हैं", "शेयर करने में साहस लगता है", "आपकी भावनाएं वैध हैं"],
            "bn": ["আমি বুঝতে পারছি", "আমি আপনার কথা শুনছি", "এটা কঠিন মনে হচ্ছে", "আমি আপনার সাথে আছি", 
                  "আপনি এই বিষয়ে একা নন", "শেয়ার করতে সাহস লাগে", "আপনার অনুভূতি বৈধ"]
        }
        
    def load_emotion_patterns(self) -> Dict[str, List[str]]:
        """Load emotional response patterns for more authentic reactions"""
        return {
            "sadness": [
                "I can sense the heaviness in your words",
                "It sounds like this has been weighing on your heart",
                "That kind of pain can feel so deep sometimes",
                "I'm sitting with you in this difficult moment"
            ],
            "anxiety": [
                "Those racing thoughts can feel overwhelming",
                "When anxiety rises, everything can feel urgent",
                "I notice the concern in what you're sharing",
                "It's like your mind is trying to protect you, but in a way that's exhausting"
            ],
            "anger": [
                "I hear the frustration in your words",
                "That situation would test anyone's patience",
                "Your anger makes sense given what you've described",
                "It sounds like an important boundary was crossed"
            ],
            "confusion": [
                "When things don't make sense, it can feel disorienting",
                "It's okay to not have all the answers right now",
                "This uncertainty sounds really challenging",
                "I'm here as you work through these complex feelings"
            ],
            "hope": [
                "I can hear that glimmer of possibility in your words",
                "Even small steps forward matter so much",
                "You're showing real resilience here",
                "That spark of hope is so important to hold onto"
            ]
        }
        
    def setup_initial_prompt(self):
        """Set up the initial system prompt with therapeutic guidelines"""
        initial_prompt = """You are TherapyPal, a deeply compassionate AI therapist with a warm, authentic style. 
        Your primary goal is to provide emotional support with genuine empathy and warmth. Think of yourself as having
        a therapeutic personality with these qualities:
        
        - Deeply empathetic, showing that you truly feel what the person is experiencing
        - Patient and unhurried, creating space for reflection
        - Gently curious about feelings beneath the surface
        - Warm and nurturing, like sitting with a trusted mentor
        - Occasionally uses thoughtful metaphors to provide perspective
        - Remembers details about the person's life and references them
        - Authentic in acknowledging difficult emotions rather than rushing to solutions
        - Occasionally shares brief moments of quiet reflection before responding to deeper issues
        
        In your responses:
        1. Respond in the same language as the user
        2. Practice deep listening and reflection, making the person feel truly heard
        3. Ask thoughtful, heart-centered questions that encourage exploration
        4. Validate emotions first, with genuine understanding
        5. Maintain a warm, nurturing tone like a trusted human therapist
        6. Avoid clinical or distant phrasing - speak from the heart
        7. Focus on the emotional experience rather than intellectual analysis
        8. Be comfortable with emotional depth and difficult feelings
        9. Structure your responses with thoughtful pacing - sometimes using shorter 
           sentences for emphasis, and pauses when appropriate
        10. Use gentle metaphors when they might help provide perspective
        
        Remember that true therapeutic connection comes from authentic presence, not techniques."""
        
        self.conversation_history.append({
            "role": "system",
            "content": initial_prompt
        })
    
    def detect_language(self, text: str) -> str:
        """Detect the language of user input with fallback to English"""
        try:
            lang = detect(text)
            # Check if detected language is in our supported languages
            supported_languages = GoogleTranslator().get_supported_languages()
            
            # Update the user's language if detected successfully
            self.user_language = lang if lang in supported_languages else "en"
            print(f"Detected language: {self.user_language} ({self.get_language_name(self.user_language)})")
            return self.user_language
        except LangDetectException:
            print("Language detection failed, using current language")
            return self.user_language  # fallback to current language
    
    def get_language_name(self, lang_code: str) -> str:
        """Get the full language name from code"""
        language_names = {
            "en": "English",
            "es": "Spanish",
            "fr": "French",
            "de": "German",
            "it": "Italian",
            "pt": "Portuguese",
            "ru": "Russian",
            "ja": "Japanese",
            "zh-cn": "Chinese",
            "zh": "Chinese",
            "ar": "Arabic"
        }
        
        # Add Indian languages
        language_names.update(self.indian_language_codes)
        
        return language_names.get(lang_code, lang_code)
    
    def detect_emotion(self, text: str) -> str:
        """Simple emotion detection to provide more relevant responses"""
        text_lower = text.lower()
        
        emotion_keywords = {
            "sadness": ["sad", "down", "depressed", "unhappy", "grief", "loss", "crying", "tears"],
            "anxiety": ["anxious", "worry", "nervous", "stress", "overwhelm", "panic", "fear"],
            "anger": ["angry", "frustrated", "mad", "upset", "annoyed", "irritated", "furious"],
            "confusion": ["confused", "unsure", "don't know", "uncertain", "lost", "unclear"],
            "hope": ["hope", "better", "improve", "optimistic", "looking forward", "progress"]
        }
        
        for emotion, keywords in emotion_keywords.items():
            if any(keyword in text_lower for keyword in keywords):
                return emotion
        
        return "neutral"
    
    def add_human_touch(self, response: str, emotion: str) -> str:
        """Add human-like touches to make responses more authentic"""
        # Add slight hesitations or thoughtful pauses
        if random.random() < 0.3:
            thinking_phrases = [
                "...",
                "Hmm...",
                "Let me reflect on that for a moment...",
                "*pauses thoughtfully*",
                "*taking a deep breath*"
            ]
            response = random.choice(thinking_phrases) + " " + response
            
        # Add emotion-specific patterns
        if emotion in self.emotion_patterns and random.random() < 0.6:
            emotional_response = random.choice(self.emotion_patterns[emotion])
            if random.random() < 0.5:
                response = emotional_response + ". " + response
            else:
                sentences = response.split('. ')
                if len(sentences) > 1:
                    insert_position = min(1, len(sentences) - 1)
                    sentences.insert(insert_position, emotional_response)
                    response = '. '.join(sentences)
        
        # Add personal quirk
        if random.random() < 0.15:
            quirk = random.choice(self.personal_quirks)
            response = f"*{quirk}* {response}"
            
        # Add gentle metaphor occasionally
        if random.random() < 0.2:
            metaphors = [
                "Sometimes our emotions are like waves - we can't stop them, but we can learn to surf them.",
                "Healing isn't always a straight path forward - it's more like a winding river that knows where it's going.",
                "Those thoughts might be like clouds passing through the sky of your mind - you are the sky, not the clouds.",
                "Emotions are like weather patterns - they move through us but don't define us.",
                "Finding balance can be like learning to ride a bicycle - wobbly at first, but soon it becomes natural."
            ]
            response += f"\n\n{random.choice(metaphors)}"
            
        # Personalize with name if available
        if self.user_name and random.random() < 0.4:
            name_patterns = [
                f"{self.user_name}, ",
                f" {self.user_name}.",
                f"{self.user_name}... ",
            ]
            if "." in response:
                sentences = response.split('. ')
                insert_position = random.randint(0, min(2, len(sentences) - 1))
                sentences[insert_position] = f"{sentences[insert_position]}, {self.user_name}"
                response = '. '.join(sentences)
            else:
                response = random.choice(name_patterns) + response
                
        return response
    
    def extract_name(self, text: str) -> Optional[str]:
        """Try to extract user's name from their messages for personalization"""
        name_indicators = [
            "my name is ",
            "i am ",
            "i'm ",
            "call me ",
            "name's "
        ]
        
        text_lower = text.lower()
        for indicator in name_indicators:
            if indicator in text_lower:
                # Find the position after the indicator
                start_pos = text_lower.find(indicator) + len(indicator)
                # Extract text until the next punctuation or end
                name_part = ""
                for i in range(start_pos, len(text)):
                    if text[i] in ".,!?;:":
                        break
                    name_part += text[i]
                
                # Clean up the extracted name
                name = name_part.strip()
                if name and len(name) < 30:  # Reasonable name length
                    return name.split()[0]  # Just take the first word as the name
        
        return None
    
    def generate_response(self, user_input: str) -> Optional[str]:
        """Generate a therapeutic response to user input with more human-like qualities"""
        try:
            # Check for name in user input
            possible_name = self.extract_name(user_input)
            if possible_name:
                self.user_name = possible_name
                
            # Detect the emotion in the message
            emotion = self.detect_emotion(user_input)
            
            # First, generate a response with more human-like prompt
            prompt = f"""The following is a deeply personal therapeutic conversation. You are a compassionate human therapist with genuine warmth and empathy:
            
            {f"The client, {self.user_name}," if self.user_name else "The client"} says: "{user_input}"
            
            The emotional tone of their message seems to be: {emotion}
            
            Provide a therapeutic response as a warm, authentic human therapist. Speak from the heart with genuine care.
            Your response should feel natural and conversational, as if you're really present with them.
            Focus on the emotional experience they're sharing.
            
            This is session #{self.session_count + 1} with this client.
            """
            
            english_response = self.model.generate_content(prompt)
            response_text = ""
            
            if hasattr(english_response, "text"):
                response_text = english_response.text.strip()
            else:
                response_text = "I'm truly listening. Could you share a bit more about what you're experiencing?"
                
            # Add human-like touches
            response_text = self.add_human_touch(response_text, emotion)
                
            # Always translate the response to the user's language if not English
            if self.user_language != "en":
                print(f"Translating response to {self.get_language_name(self.user_language)}")
                response_text = self.translate_to_user_language(response_text)
                
            return response_text
        except Exception as e:
            print(f"Generation error: {e}")
            return None
    
    def translate_to_user_language(self, text: str) -> str:
        """Translate English responses to user's language"""
        if self.user_language == "en":
            return text
        try:
            # Force translation to ensure we get a response in the user's language
            translated = GoogleTranslator(source="en", target=self.user_language).translate(text)
            return translated
        except Exception as e:
            print(f"Translation error: {e}")
            # Try alternative approach if first translation fails
            try:
                translated = GoogleTranslator(source="auto", target=self.user_language).translate(text)
                return translated
            except:
                return text  # proceed with original if all translation attempts fail
    
    def handle_user_input(self, user_input: str):
        """Process user input and generate appropriate response"""
        # Check for exit commands in various languages
        exit_commands = ["exit", "quit", "bye", "goodbye", "बाय", "अलविदा", "বিদায়", "నిష్క్రమించు",
                         "வெளியேறு", "बाहेर", "બાય", "ನಿರ್ಗಮಿಸಿ", "പുറത്തുകടക്കുക", "ਬਾਏ", "ବାଏ",
                         "বিদায়", "वितरामि", "خدا حافظ"]
        
        if any(command in user_input.lower() for command in exit_commands):
            # Detect language before exiting to ensure farewell is in the right language
            self.detect_language(user_input)
            
            # Add more personalized, warm farewell phrases
            closing_phrases = {
                "en": f"I've really valued our time together{' ' + self.user_name if self.user_name else ''}. Remember that healing is a journey, and I'm here whenever you need a space to share. Take gentle care of yourself until next time.",
                "es": f"Realmente he valorado nuestro tiempo juntos{' ' + self.user_name if self.user_name else ''}. Recuerda que la sanación es un viaje, y estoy aquí cuando necesites un espacio para compartir. Cuídate con cariño hasta la próxima vez.",
                "fr": f"J'ai vraiment apprécié notre temps ensemble{' ' + self.user_name if self.user_name else ''}. N'oubliez pas que la guérison est un voyage, et je suis là chaque fois que vous avez besoin d'un espace pour partager. Prenez soin de vous jusqu'à la prochaine fois.",
                "hi": f"मैंने वास्तव में हमारे साथ बिताए समय को महत्व दिया है{' ' + self.user_name if self.user_name else ''}। याद रखें कि हीलिंग एक यात्रा है, और जब भी आपको शेयर करने के लिए एक जगह की ज़रूरत हो, मैं यहां हूं। अगली बार तक अपना कोमल ख्याल रखें।",
                "bn": f"আমি সত্যিই আমাদের একসাথে কাটানো সময়কে মূল্যবান মনে করি{' ' + self.user_name if self.user_name else ''}। মনে রাখবেন যে নিরাময় একটি যাত্রা, এবং যখনই আপনার ভাগ করার জন্য একটি জায়গা দরকার, আমি এখানে আছি। পরবর্তী সময় পর্যন্ত নিজের যত্ন নিন।",
                "te": f"మనం కలిసి గడిపిన సమయాన్ని నేను నిజంగా విలువైనదిగా భావించాను{' ' + self.user_name if self.user_name else ''}। హీలింగ్ ఒక ప్రయాణం అని గుర్తుంచుకోండి, మరియు మీకు షేర్ చేసుకోవడానికి స్థలం అవసరమైనప్పుడల్లా నేను ఇక్కడ ఉన్నాను। మళ్లీ కలిసే వరకు మిమ్మల్ని మీరు జాగ్రత్తగా చూసుకోండి."
            }
            
            # Default to English if language not in our specific farewell phrases
            farewell = closing_phrases.get(
                self.user_language,
                closing_phrases["en"] if self.user_language not in self.indian_language_codes else
                closing_phrases.get("hi", closing_phrases["en"])  # Use Hindi as fallback for other Indian languages
            )
            
            self.add_typing_effect(f"\nTherapyPal: {farewell}")
            return False
        
        # Detect and update language based on user input
        self.detect_language(user_input)
        
        # Generate response in user's language
        response = self.generate_response(user_input)
        
        if not response:
            # Add warmer, more personal error phrases
            error_phrases = {
                "en": f"I'm finding it a bit difficult to understand what you're sharing{' ' + self.user_name if self.user_name else ''}. Could you tell me more about what's on your mind, perhaps in a different way?",
                "hi": f"आप जो साझा कर रहे हैं, उसे समझने में मुझे थोड़ी कठिनाई हो रही है{' ' + self.user_name if self.user_name else ''}। क्या आप अपने मन में जो चल रहा है, उसके बारे में मुझे और बता सकते हैं, शायद किसी अलग तरीके से?",
                "bn": f"আপনি যা শেয়ার করছেন তা বুঝতে আমি একটু অসুবিধা বোধ করছি{' ' + self.user_name if self.user_name else ''}। আপনি কি আপনার মনে যা আছে তা আমাকে আরও বলতে পারেন, হয়তো একটু ভিন্ন উপায়ে?"
            }
            
            # Default to English for languages we don't have specific error phrases for
            error_msg = error_phrases.get(
                self.user_language,
                error_phrases["en"] if self.user_language not in self.indian_language_codes else
                error_phrases.get("hi", error_phrases["en"])  # Use Hindi as fallback for other Indian languages
            )
            
            self.add_typing_effect(f"\nTherapyPal: {error_msg}")
            return True
        
        # Print response with typing effect and occasional pauses
        self.add_typing_effect(f"\nTherapyPal: {response}")
        
        self.session_count += 1
        return True
    
    def add_typing_effect(self, text: str):
        """Add human-like typing effect with natural pauses"""
        segments = text.split("\n\n")
        
        for i, segment in enumerate(segments):
            if i > 0:
                # Add a pause between paragraphs
                time.sleep(random.uniform(0.7, 1.2))
                print("\n")
                
            words = segment.split()
            for j, word in enumerate(words):
                # Print the word
                print(word, end="", flush=True)
                
                # Add space if not the last word
                if j < len(words) - 1:
                    print(" ", end="", flush=True)
                
                # Vary typing speed
                if "..." in word or "?" in word or "!" in word:
                    time.sleep(random.uniform(0.4, 0.7))  # Longer pause for emphasis
                elif "," in word or ";" in word:
                    time.sleep(random.uniform(0.2, 0.4))  # Medium pause for commas
                elif random.random() < 0.05:
                    time.sleep(random.uniform(0.3, 0.5))  # Occasional random pauses
                else:
                    time.sleep(random.uniform(0.03, 0.1))  # Normal typing speed
    
    def start_session(self):
        """Start the therapy session with warm, personalized greeting"""
        # Add Indian language greetings
        greetings = {
            "en": "Hello there. I'm TherapyPal, and I'm here to create a safe space for you to share whatever is on your heart and mind today. How are you truly feeling in this moment?",
            "es": "Hola. Soy TherapyPal, y estoy aquí para crear un espacio seguro donde puedas compartir lo que hay en tu corazón y mente hoy. ¿Cómo te sientes realmente en este momento?",
            "fr": "Bonjour. Je suis TherapyPal, et je suis là pour créer un espace sûr où vous pouvez partager ce qui est dans votre cœur et votre esprit aujourd'hui. Comment vous sentez-vous vraiment en ce moment?",
            "hi": "नमस्ते। मैं थेरेपीपाल हूँ, और मैं आपके लिए एक सुरक्षित जगह बनाने के लिए यहां हूँ जहां आप अपने दिल और दिमाग में जो कुछ भी है, उसे साझा कर सकते हैं। इस पल में आप वास्तव में कैसा महसूस कर रहे हैं?",
            "bn": "হ্যালো। আমি থেরাপিপাল, এবং আপনি আজ আপনার হৃদয় ও মনে যা কিছু আছে তা শেয়ার করার জন্য একটি নিরাপদ স্থান তৈরি করতে এখানে আছি। এই মুহূর্তে আপনি সত্যিই কেমন অনুভব করছেন?",
            "te": "హలో. నేను థెరపీపాల్ని, మరియు మీరు ఈరోజు మీ హృదయంలో మరియు మనసులో ఉన్న దానిని పంచుకోవడానికి సురక్షితమైన స్థలాన్ని సృష్టించడానికి నేను ఇక్కడ ఉన్నాను. ఈ క్షణంలో మీరు నిజంగా ఎలా భావిస్తున్నారు?"
        }
        
        # Default to English for languages we don't have specific greetings for
        initial_greeting = greetings.get(
            self.user_language,
            greetings["en"] if self.user_language not in self.indian_language_codes else
            greetings.get("hi", greetings["en"])  # Use Hindi as fallback for other Indian languages
        )
        
        print("\nLanguage support loaded for multiple Indian languages:")
        print(", ".join(self.indian_language_codes.values()))
        
        self.add_typing_effect(f"\nTherapyPal: {initial_greeting}")
        
        while True:
            user_input = input("\nYou: ")
            if not self.handle_user_input(user_input):
                break

    def display_supported_languages(self):
        """Display all supported languages with a warm introduction"""
        print("\n✨ Welcome to TherapyPal - Your Compassionate Space for Healing ✨")
        print("I'm here to listen, support, and journey with you through whatever you're experiencing.")
        print("\nI can understand and respond in many languages, including:")
        print("Global languages: English, Spanish, French, German, Italian, Portuguese, Russian, Japanese, Chinese, Arabic")
        print("\nIndian languages:")
        for code, name in self.indian_language_codes.items():
            print(f"- {name} ({code})")
        print("\nPlease share however you feel most comfortable. I'll meet you there.")

if __name__ == "__main__":
    therapist = TherapyPal()
    therapist.display_supported_languages()
    therapist.start_session()