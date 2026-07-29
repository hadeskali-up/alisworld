# Push AlisWorld to GitHub

## Quick Steps

### 1. Create GitHub Repo (Manual)

1. Go to https://github.com/new
2. Repository name: `alisworld`
3. Description: `MT5 Forex Trade Monitor & Manager - Android App`
4. Visibility: **Public**
5. **DO NOT** initialize with README, .gitignore, or license (repo sudah ada ini semua)
6. Click "Create repository"

### 2. Push from Terminal

```bash
cd C:\Users\User\alisworld

# Check current remote
git remote -v

# If remote already points to alisuhari/alisworld (it does):
git push -u origin master

# If you need to change remote:
git remote set-url origin https://github.com/YOUR_USERNAME/alisworld.git
git push -u origin master
```

### 3. Verify

After push, check https://github.com/alisuhari/alisworld

You should see:
- 8 commits
- 3 folders: backend/, pusher/, android/
- 8 markdown docs
- README.md displayed

---

## If GitHub Asks for Authentication

### Option A: Personal Access Token (Recommended)
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token → Select scopes: `repo`
3. Copy token
4. When git push asks for password, paste the token

### Option B: SSH Key
```bash
# Generate SSH key
ssh-keygen -t ed25519 -C "your_email@example.com"

# Add to SSH agent
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519

# Copy public key
cat ~/.ssh/id_ed25519.pub

# Add to GitHub: Settings → SSH and GPG keys → New SSH key

# Change remote to SSH
cd C:\Users\User\alisworld
git remote set-url origin git@github.com:alisuhari/alisworld.git
git push -u origin master
```

---

## After Successful Push

Your repo will be live at:
**https://github.com/alisuhari/alisworld**

Then you can:
1. Set up GitHub Actions secrets for auto APK build
2. Push updates: `git push`
3. Clone on other machines: `git clone https://github.com/alisuhari/alisworld.git`

---

## Current Status

- ✅ Git repo: 8 commits ready
- ✅ Remote configured: alisuhari/alisworld
- ✅ Branch: master
- ⏳ GitHub repo: Need to create manually
- ⏳ Push: Ready after repo created

Run `git push -u origin master` after creating the GitHub repo.
