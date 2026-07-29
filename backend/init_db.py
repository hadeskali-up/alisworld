import sqlite3
from config import settings


def init_database():
    """Create all tables"""
    db_path = settings.database_url.replace("sqlite:///", "").replace("./", "")
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # Account snapshots
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS account_snapshots (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            balance REAL NOT NULL,
            equity REAL NOT NULL,
            margin REAL NOT NULL,
            free_margin REAL NOT NULL,
            margin_level REAL NOT NULL,
            currency TEXT NOT NULL,
            daily_pnl REAL NOT NULL,
            daily_pnl_pct REAL NOT NULL,
            server_time TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)

    # Open positions (full replace on each push)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS open_positions (
            ticket INTEGER PRIMARY KEY,
            symbol TEXT NOT NULL,
            type TEXT NOT NULL,
            volume REAL NOT NULL,
            open_price REAL NOT NULL,
            current_price REAL NOT NULL,
            sl REAL NOT NULL,
            tp REAL NOT NULL,
            swap REAL NOT NULL,
            commission REAL NOT NULL,
            profit REAL NOT NULL,
            open_time TEXT NOT NULL,
            comment TEXT,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)

    # Trade history (append-only)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS trade_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ticket INTEGER NOT NULL,
            symbol TEXT NOT NULL,
            type TEXT NOT NULL,
            volume REAL NOT NULL,
            open_price REAL NOT NULL,
            close_price REAL NOT NULL,
            swap REAL NOT NULL,
            commission REAL NOT NULL,
            profit REAL NOT NULL,
            result TEXT NOT NULL,
            close_reason TEXT NOT NULL,
            open_time TEXT NOT NULL,
            close_time TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE(ticket)
        )
    """)

    # Command queue
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS commands (
            command_id TEXT PRIMARY KEY,
            action TEXT NOT NULL,
            ticket INTEGER NOT NULL,
            status TEXT DEFAULT 'pending',
            closed_price REAL,
            message TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            completed_at TIMESTAMP
        )
    """)

    # FCM device tokens for push notifications
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS fcm_tokens (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            token TEXT UNIQUE NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)

    conn.commit()
    conn.close()
    print("Database initialized successfully")


if __name__ == "__main__":
    init_database()
