const url = "https://lottiefiles.com/free-animation/chatgpt-atlas-animation-mockup-JAD74oUkdb";
fetch(url, {
    headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Accept": "text/html"
    }
}).then(res => res.text()).then(html => {
    const lottieMatch = html.match(/https:\/\/[^\s"'<>]+\.lottie/);
    const jsonMatch = html.match(/https:\/\/[^\s"'<>]+\.json/);
    console.log("Lottie URL:", lottieMatch ? lottieMatch[0] : "Not found");
    console.log("JSON URL:", jsonMatch ? jsonMatch[0] : "Not found");
}).catch(err => console.error(err));
