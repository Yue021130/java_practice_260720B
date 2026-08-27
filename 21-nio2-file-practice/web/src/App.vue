<template>
  <div class="app">
    <header>
      <h1>Java NIO.2 文件操作实战</h1>
      <p>Path + Files 结合 Spring Boot + Vue3，覆盖公众号原文全部示例</p>
    </header>

    <nav class="tabs">
      <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="{ active: currentTab === tab.key }"
          @click="currentTab = tab.key"
      >{{ tab.label }}</button>
    </nav>

    <main>
      <!-- 路径运算 -->
      <section v-if="currentTab === 'path'">
        <h2>Path 四兄弟</h2>
        <div class="card">
          <label>base <input v-model="pathForm.base" placeholder="docs"/></label>
          <label>other <input v-model="pathForm.other" placeholder="report.txt"/></label>
          <button @click="call('resolve', () => api.resolve(pathForm.base, pathForm.other))">resolve</button>
        </div>
        <div class="card">
          <label>path <input v-model="pathForm.path" placeholder="a.txt"/></label>
          <label>sibling <input v-model="pathForm.sibling" placeholder="b.txt"/></label>
          <button @click="call('resolveSibling', () => api.resolveSibling(pathForm.path, pathForm.sibling))">resolveSibling</button>
        </div>
        <div class="card">
          <label>from <input v-model="pathForm.from" placeholder="docs"/></label>
          <label>to <input v-model="pathForm.to" placeholder="docs/report.txt"/></label>
          <button @click="call('relativize', () => api.relativize(pathForm.from, pathForm.to))">relativize</button>
        </div>
        <div class="card">
          <label>path <input v-model="pathForm.normalize" placeholder="a/../b/./c.txt"/></label>
          <button @click="call('normalize', () => api.normalize(pathForm.normalize))">normalize</button>
        </div>
        <div class="card">
          <label>path <input v-model="pathForm.file" placeholder="hello.txt"/></label>
          <button @click="call('toAndFromFile', () => api.toAndFromFile(pathForm.file))">Path ⇄ File</button>
        </div>
      </section>

      <!-- 创建与删除 -->
      <section v-if="currentTab === 'create'">
        <h2>创建 / 删除</h2>
        <div class="card">
          <label>path <input v-model="createForm.path" placeholder="dir/file.txt"/></label>
          <label>type
            <select v-model="createForm.type">
              <option value="FILE">FILE</option>
              <option value="DIRECTORY">DIRECTORY</option>
              <option value="TEMP_FILE">TEMP_FILE</option>
              <option value="TEMP_DIR">TEMP_DIR</option>
            </select>
          </label>
          <label>prefix <input v-model="createForm.prefix" placeholder="tmp-"/></label>
          <label>suffix <input v-model="createForm.suffix" placeholder=".tmp"/></label>
          <button @click="call('create', () => api.create(createForm))">create</button>
        </div>
        <div class="card">
          <label>path <input v-model="deleteForm.path" placeholder="dir"/></label>
          <label><input type="checkbox" v-model="deleteForm.recursive"/> recursive</label>
          <button class="danger" @click="call('delete', () => api.delete(deleteForm))">delete</button>
        </div>
      </section>

      <!-- 读写 -->
      <section v-if="currentTab === 'readwrite'">
        <h2>读写文本</h2>
        <div class="card">
          <label>path <input v-model="writeForm.path" placeholder="hello.txt"/></label>
          <textarea v-model="writeForm.content" rows="5" placeholder="输入文件内容"></textarea>
          <button @click="call('write', () => api.write(writeForm))">write</button>
        </div>
        <div class="card">
          <label>path <input v-model="readPath" placeholder="hello.txt"/></label>
          <button @click="call('read', () => api.read(readPath))">read</button>
          <button @click="call('lines', () => api.lines(readPath))">read lines</button>
          <button @click="call('lineStats', () => api.lineStats(readPath))">line stats</button>
        </div>
        <div class="card">
          <label>path <input v-model="linesForm.path" placeholder="multi.txt"/></label>
          <textarea v-model="linesForm.lines" rows="5" placeholder="每行一条"></textarea>
          <button @click="call('writeLines', () => api.writeLines({path: linesForm.path, lines: linesForm.lines.split('\\n')}))">writeLines</button>
          <button @click="call('readLinesBuffered', () => api.readLinesBuffered(linesForm.path))">readLinesBuffered</button>
        </div>
      </section>

      <!-- Properties -->
      <section v-if="currentTab === 'properties'">
        <h2>Properties 文件</h2>
        <div class="card">
          <label>path <input v-model="propsForm.path" placeholder="app.properties"/></label>
          <textarea v-model="propsForm.content" rows="6" placeholder="key=value 每行一个"></textarea>
          <button @click="writeProperties">writeProperties</button>
          <button @click="call('readProperties', () => api.readProperties(propsForm.path))">readProperties</button>
        </div>
      </section>

      <!-- 复制与移动 -->
      <section v-if="currentTab === 'copymove'">
        <h2>复制 / 移动 / 上传</h2>
        <div class="card">
          <label>src <input v-model="copyForm.src" placeholder="a.txt"/></label>
          <label>dst <input v-model="copyForm.dst" placeholder="b.txt"/></label>
          <label><input type="checkbox" v-model="copyForm.replaceExisting"/> replaceExisting</label>
          <label><input type="checkbox" v-model="copyForm.copyAttributes"/> copyAttributes</label>
          <button @click="call('copy', () => api.copy(copyForm))">copy</button>
        </div>
        <div class="card">
          <label>src dir <input v-model="copyDirForm.src" placeholder="dir"/></label>
          <label>dst dir <input v-model="copyDirForm.dst" placeholder="dir-copy"/></label>
          <button @click="call('copyDirectory', () => api.copyDirectory(copyDirForm))">copyDirectory</button>
        </div>
        <div class="card">
          <label>src <input v-model="moveForm.src" placeholder="a.txt"/></label>
          <label>dst <input v-model="moveForm.dst" placeholder="b.txt"/></label>
          <label><input type="checkbox" v-model="moveForm.atomic"/> atomic</label>
          <button @click="call('move', () => api.move(moveForm))">move</button>
        </div>
        <div class="card">
          <label>目标路径 <input v-model="uploadDst" placeholder="uploads/file.txt"/></label>
          <input type="file" @change="onFileChange"/>
          <button @click="upload">upload</button>
        </div>
      </section>

      <!-- 遍历 -->
      <section v-if="currentTab === 'traverse'">
        <h2>目录遍历</h2>
        <div class="card">
          <label>dir <input v-model="traverseDir" placeholder=""/></label>
          <button @click="call('list', () => api.list(traverseDir))">list</button>
          <button @click="call('walk', () => api.walk(traverseDir, 0))">walk</button>
          <button @click="call('stats', () => api.stats(traverseDir))">stats</button>
        </div>
      </section>

      <!-- 属性 -->
      <section v-if="currentTab === 'attr'">
        <h2>文件属性</h2>
        <div class="card">
          <label>path <input v-model="attrPath" placeholder="hello.txt"/></label>
          <button @click="call('properties', () => api.properties(attrPath))">properties</button>
        </div>
      </section>

      <!-- 八股 -->
      <section v-if="currentTab === 'essay'">
        <h2>八股速记</h2>
        <button @click="call('explain', () => api.explain())">加载核心考点</button>
      </section>

      <!-- 结果展示 -->
      <div class="result">
        <h3>结果</h3>
        <pre v-if="result">{{ JSON.stringify(result, null, 2) }}</pre>
        <p v-if="error" class="error">{{ error }}</p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import api from './api/nio.js'

const tabs = [
  { key: 'path', label: 'Path 运算' },
  { key: 'create', label: '创建/删除' },
  { key: 'readwrite', label: '读写' },
  { key: 'properties', label: 'Properties' },
  { key: 'copymove', label: '复制/移动' },
  { key: 'traverse', label: '遍历' },
  { key: 'attr', label: '属性' },
  { key: 'essay', label: '八股' }
]

const currentTab = ref('path')
const result = ref(null)
const error = ref('')

const pathForm = reactive({ base: '', other: 'report.txt', path: 'a.txt', sibling: 'b.txt', from: '', to: 'report.txt', normalize: 'a/../b/./c.txt', file: 'hello.txt' })
const createForm = reactive({ path: 'demo.txt', type: 'FILE', prefix: '', suffix: '' })
const deleteForm = reactive({ path: '', recursive: false })
const writeForm = reactive({ path: 'hello.txt', content: '你好，NIO.2！' })
const readPath = ref('hello.txt')
const linesForm = reactive({ path: 'multi.txt', lines: '第一行\n第二行\n第三行' })
const propsForm = reactive({ path: 'app.properties', content: 'app.name=nio-practice\napp.version=1.0' })
const copyForm = reactive({ src: 'hello.txt', dst: 'hello-copy.txt', replaceExisting: true, copyAttributes: false })
const copyDirForm = reactive({ src: 'dir', dst: 'dir-copy' })
const moveForm = reactive({ src: 'hello.txt', dst: 'hello-moved.txt', atomic: false })
const uploadDst = ref('uploads/uploaded.txt')
const traverseDir = ref('')
const attrPath = ref('hello.txt')

let uploadFile = null

async function call(name, fn) {
  result.value = null
  error.value = ''
  try {
    const res = await fn()
    if (res.code !== 200) {
      throw new Error(res.message || '业务失败')
    }
    result.value = res.data
  } catch (e) {
    error.value = e.message
  }
}

function onFileChange(e) {
  uploadFile = e.target.files[0]
}

async function upload() {
  if (!uploadFile) {
    error.value = '请先选择文件'
    return
  }
  await call('upload', () => api.upload(uploadFile, uploadDst.value))
}

function writeProperties() {
  const map = {}
  propsForm.content.split('\n').forEach(line => {
    const idx = line.indexOf('=')
    if (idx > 0) {
      map[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
    }
  })
  call('writeProperties', () => api.writeProperties({ path: propsForm.path, properties: map }))
}
</script>

<style>
* { box-sizing: border-box; }
body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background: #f5f7fa; color: #333; }
.app { max-width: 960px; margin: 0 auto; padding: 20px; }
header { text-align: center; margin-bottom: 20px; }
header h1 { margin: 0; font-size: 28px; }
header p { color: #666; margin-top: 6px; }
.tabs { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px; }
.tabs button { padding: 8px 16px; border: none; background: #fff; border-radius: 6px; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
.tabs button.active { background: #409eff; color: #fff; }
section h2 { font-size: 20px; margin-bottom: 12px; border-left: 4px solid #409eff; padding-left: 10px; }
.card { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.card label { display: inline-block; margin-right: 12px; margin-bottom: 8px; }
.card input, .card select, .card textarea { padding: 6px 10px; border: 1px solid #d9d9d9; border-radius: 4px; margin-left: 6px; min-width: 180px; }
.card textarea { display: block; width: 100%; margin: 10px 0; }
.card button { padding: 6px 16px; border: none; background: #409eff; color: #fff; border-radius: 4px; cursor: pointer; margin-right: 8px; }
.card button.danger { background: #f56c6c; }
.result { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.result pre { background: #1e1e1e; color: #d4d4d4; padding: 12px; border-radius: 6px; overflow-x: auto; }
.error { color: #f56c6c; }
</style>
