<!DOCTYPE html>
<html lang="zh-Hant">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>歡迎</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 0;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            background: linear-gradient(135deg, #ff9a9e, #fad0c4);
            color: #333;
        }
        .welcome-container {
            text-align: center;
            background: rgba(255, 255, 255, 0.9);
            padding: 20px 40px;
            border-radius: 15px;
            box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
        }
        .welcome-container h1 {
            font-size: 2.5em;
            margin-bottom: 10px;
        }
        .welcome-container p {
            font-size: 1.2em;
            margin-bottom: 20px;
        }
        .welcome-container button {
            background-color: #ff6f61;
            color: white;
            border: none;
            padding: 10px 20px;
            font-size: 1em;
            border-radius: 5px;
            cursor: pointer;
            transition: background-color 0.3s;
        }
        .welcome-container button:hover {
            background-color: #e55b50;
        }
    </style>
</head>
<body>
    <div class="welcome-container">
        <h1>歡迎來到我們的網站！</h1>
        <p>我們很高興您能光臨，希望您有愉快的瀏覽體驗。</p>
        <button onclick="alert('感謝您的到來！')">開始探索</button>
    </div>
</body>
</html>
