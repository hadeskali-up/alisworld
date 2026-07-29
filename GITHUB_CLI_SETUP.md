# GitHub CLI Setup & Push

## ✅ GitHub CLI Installed

Location: `/c/Users/User/Downloads/bin/gh.exe`
Version: 2.62.0

---

## 🔐 Step 1: Authenticate (Choose One Method)

### Method A: Web Browser (Easiest)

```bash
/c/Users/User/Downloads/bin/gh.exe auth login --web
```

When it shows the code (e.g., `E41F-A1BC`):
1. Open: https://github.com/login/device
2. Enter the code
3. Click "Authorize"
4. Come back to terminal - it will complete

### Method B: Token (Alternative)

```bash
# 1. Create token: https://github.com/settings/tokens/new
#    Scopes: repo, workflow
# 2. Copy the token
# 3. Run:
/c/Users/User/Downloads/bin/gh.exe auth login --with-token
# Paste token when prompted
```

---

## 🚀 Step 2: Create Repo & Push

After authentication succeeds, run:

```bash
cd /c/Users/User/alisworld
/c/Users/User/Downloads/bin/gh.exe repo create alisworld \
  --public \
  --source=. \
  --remote=origin \
  --push \
  --description="MT5 Forex Trade Monitor & Manager - Android App"
```

This will:
1. Create `github.com/YOUR_USERNAME/alisworld` repo
2. Set it as remote origin
3. Push all 9 commits
4. Open in browser

---

## 🔍 Verify

After push completes:
```bash
/c/Users/User/Downloads/bin/gh.exe repo view --web
```

---

## ⚡ Quick Copy-Paste Commands

```bash
# Authenticate
/c/Users/User/Downloads/bin/gh.exe auth login --web

# Create & push (after auth)
cd /c/Users/User/alisworld && /c/Users/User/Downloads/bin/gh.exe repo create alisworld --public --source=. --remote=origin --push --description="MT5 Forex Trade Monitor & Manager"
```

---

## Current Status

✅ GitHub CLI: Installed at `/c/Users/User/Downloads/bin/gh.exe`  
⏳ Authentication: Waiting for you to complete  
⏳ Repo creation: Ready after auth  
⏳ Push: Ready after repo created  

**Next**: Run the auth command and follow the prompts.
