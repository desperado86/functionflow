#!/bin/bash

echo "🚀 启动函数流工作引擎..."

# 检查Java版本
echo "📋 检查Java版本..."
java -version

# 编译项目
echo "🔨 编译项目..."
mvn clean compile

# 运行测试
echo "🧪 运行测试..."
mvn test

# 启动应用
echo "🌟 启动Spring Boot应用..."
          echo "📱 应用将在 http://localhost:8099 启动"
          echo "🎨 工作流设计器: http://localhost:8099/workflow-designer.html"
          echo "🚀 高级设计器: http://localhost:8099/workflow-designer-advanced.html"
          echo "📋 函数列表: http://localhost:8099/functions"
          echo "🔧 API文档: http://localhost:8099/api/functions"

          mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8099"
