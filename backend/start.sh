#!/bin/bash
# Quick start script for AlisWorld backend

echo "=== AlisWorld Backend Quick Start ==="
echo

# Check if in backend directory
if [ ! -f "main.py" ]; then
    echo "Error: Run this script from backend/ directory"
    exit 1
fi

# Create .env if missing
if [ ! -f ".env" ]; then
    echo "Creating .env from template..."
    cp .env.example .env
    echo "⚠️  IMPORTANT: Edit .env and set your API_KEY"
    echo
fi

# Create venv if missing
if [ ! -d "venv" ]; then
    echo "Creating Python virtual environment..."
    python -m venv venv
fi

# Activate venv
source venv/bin/activate 2>/dev/null || source venv/Scripts/activate 2>/dev/null

# Install dependencies
echo "Installing dependencies..."
pip install -q -r requirements.txt

# Initialize database
if [ ! -f "alisworld.db" ]; then
    echo "Initializing database..."
    python init_db.py
fi

echo
echo "✅ Setup complete!"
echo
echo "Start server:"
echo "  uvicorn main:app --reload"
echo
echo "API docs: http://localhost:8000/docs"
