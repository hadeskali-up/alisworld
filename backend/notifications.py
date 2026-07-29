import firebase_admin
from firebase_admin import credentials, messaging
from config import settings
from typing import List
import os


# Initialize Firebase Admin SDK
def init_firebase():
    """Initialize Firebase Admin SDK for FCM"""
    if not settings.firebase_credentials_path:
        print("⚠️  Firebase credentials path not set — notifications disabled")
        return False
    
    cred_path = settings.firebase_credentials_path
    if not os.path.exists(cred_path):
        print(f"⚠️  Firebase credentials file not found at {cred_path} — notifications disabled")
        return False
    
    try:
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
        print("✓ Firebase initialized — notifications enabled")
        return True
    except Exception as e:
        print(f"⚠️  Firebase initialization failed: {e} — notifications disabled")
        return False


firebase_enabled = False


async def send_tp_notification(tokens: List[str], symbol: str, profit: float, close_price: float):
    """Send FCM notification for TP-hit close"""
    if not firebase_enabled or not tokens:
        return
    
    try:
        message = messaging.MulticastMessage(
            notification=messaging.Notification(
                title=f"🎯 TP Hit: {symbol}",
                body=f"Closed at {close_price:.5f} • Profit: ${profit:.2f}"
            ),
            data={
                "type": "tp_hit",
                "symbol": symbol,
                "profit": str(profit),
                "close_price": str(close_price)
            },
            tokens=tokens
        )
        
        response = messaging.send_multicast(message)
        print(f"✓ Sent TP notification to {response.success_count}/{len(tokens)} devices")
    except Exception as e:
        print(f"⚠️  Failed to send notification: {e}")
