# Simmer

A recipe app for Android. Paste a link (or share a page from Chrome), and Simmer downloads the recipe and saves it as plain text on your phone. Sort and search your recipes, sort them into categories, star favourites, and build a grocery list from the ones you're cooking this week.

Everything is stored on the phone. No account, no server.

**And it feeds people.** Simmer is free for everyone. Ad revenue and tips go to a named food bank (see `Config.kt`). Watch a short ad to fund a meal, round up after a grocery run, or tip directly. Your "table" fills with dishes as the meals you've funded add up.

## Features

- **Import from a link** (or share from Chrome): title, photo, ingredients, steps, times.
- **Cook mode**: one step at a time, big text, screen stays on, tap-to-start timers pulled from the step text.
- **Scale servings** with +/- and every quantity updates.
- **Pantry**: type what you have and see which saved recipes you can make now, and what's missing.
- **Grocery list** grouped by recipe, with a progress bar and "plan from recipes".
- **Give**: watch-to-feed (rewarded ads), tip jar, streaks, chef levels, community pot, and a plain-language "where the money goes" section.
- Share any recipe as clean text; share your impact.

## Going live checklist

1. **Google Play developer account** ($25 one-time) at play.google.com/console.
2. **AdMob** account at admob.google.com. Create an app and a Rewarded ad unit, then replace the two test ids in `Config.kt` and `AndroidManifest.xml`.
3. **Charity**: confirm the name, donation link and meals-per-dollar figure in `Config.kt` with the food bank, and get their OK to be named.
4. **Privacy policy**: host `docs/privacy.md` somewhere public (GitHub Pages works) and paste the link into the Play listing.
5. **Signing key**: `app/simmer-release.jks` is the key every build uses. Back it up somewhere safe; losing it means you can never update the app on the store.
6. **Play Billing** (optional): replace the donate-link tips with in-app purchases once the listing exists.

## Getting the app onto your phone

You don't need Android Studio. GitHub will build the APK for you.

1. Create a free account at github.com if you don't have one.
2. Make a new repository (any name, e.g. `simmer`). Leave it empty.
3. Upload this whole folder to it. Easiest way: on the repository page choose **Add file → Upload files**, drag the contents of this folder in, and commit. (Make sure the `.github` folder comes along; on some computers it's hidden.)
4. Open the **Actions** tab. A workflow called **Build APK** starts on its own. Wait for the green tick (about 5 to 10 minutes the first time).
5. Click the finished run, scroll to **Artifacts**, and download **simmer-apk**. Unzip it to get `app-release.apk`.
6. Open that file on your phone. Android will ask you to allow installs from this source. Say yes, then install.

If you prefer Android Studio: open the folder, let it sync, then **Run**.

## Using it

- **Add**: paste a recipe link and tap Import. Or in Chrome, open a recipe, tap Share, and pick Simmer.
- **Recipes**: search by name or ingredient, sort (newest, A to Z, category), filter by category chips, star favourites.
- **Grocery**: add items by hand, add a recipe's ingredients from its page, or tap *Plan from recipes* to pick several at once.

## How importing works

Nearly every recipe site publishes its recipe in a hidden structured block for Google (schema.org "Recipe"). Simmer reads that first, so you get clean ingredients and steps. If a site doesn't have it, Simmer falls back to looking for "Ingredients" and "Instructions" headings in the page text. A few sites block automated downloads; for those you can still type the recipe in by hand.
