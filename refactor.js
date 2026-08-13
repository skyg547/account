const fs = require('fs');

let content = fs.readFileSync('docker-compose.yml', 'utf8');

const pattern = /build:\s*\n\s*context:\s*\.\s*\n\s*dockerfile:\s*Containerfile(?:\.dev)?\s*\n\s*args:\s*\n\s*GRADLE_PROJECT:\s*"?([^"\n]+)"?\s*\n\s*JAR_DIRECTORY:\s*"?([^"\n]+)"?/g;

let newContent = content.replace(pattern, (match, gradleProject, jarDirectory) => {
    const jarDirClean = jarDirectory.replace(/"/g, '').trim();
    return `build:\n      context: .\n      dockerfile: ${jarDirClean}/Dockerfile`;
});

fs.writeFileSync('docker-compose.yml', newContent, 'utf8');
console.log("Updated docker-compose.yml");
