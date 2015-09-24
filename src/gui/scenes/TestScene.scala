package gui.scenes

import gui.Scene
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import org.lwjgl.opengl.GL33._
import org.lwjgl.opengl.GL40._
import org.lwjgl.opengl.GL43._
import util.GlInfo
import java.io.File
import util.ShaderProgram
import util.ShaderFactory
import scala.util.Failure
import scala.util.Success
import org.lwjgl.BufferUtils
import util.GlUtils
import org.lwjgl.opengl.GL31

class TestScene extends Scene {
  val shaderPath = new File(getClass().getResource("/shaders/mdi_test").toURI())
  
  val indirectData = Array(
      3, 1, 0, 0,
      6, 4, 3, 1
  )
  
  val vertexData = Array(
      -0.5f, -0.5f, 0f,
       0.5f, -0.5f, 0f,
         0f,  0.5f, 0f,
         
      //--------------
       
       -1f, -1f, 0f,
       1f,  -1f, 0f,
       1f,   1f, 0f,
       
       -1f, -1f, 0f,
        1f,  1f, 0f,
       -1f,  1f, 0f
  )
  
  // column major!
  val modelData = Array(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
    
    0.1f,    0f,    0f,   0f,
    0f,    0.1f,    0f,   0f,
    0f,      0f,  0.1f,   0f,
    -0.8f, -0.8f,   0f,   1f,
    
    0.1f,    0f,   0f,   0f,
    0f,    0.1f,   0f,   0f,
    0f,      0f, 0.1f,   0f,
    -0.8f, 0.8f,   0f,   1f,
    
    0.1f,    0f,   0f,   0f,
    0f,    0.1f,   0f,   0f,
    0f,      0f, 0.1f,   0f,
    0.8f, -0.8f,   0f,   1f,
    
    0.1f,   0f,   0f,   0f,
    0f,   0.1f,   0f,   0f,
    0f,     0f, 0.1f,   0f,
    0.8f, 0.8f,   0f,   1f
  )
  
  var prog: ShaderProgram = null
  var vaoId = 0
  var vboId = 0
  var mboId = 0
  var iboId = 0
  
  def start(): SceneStatus = {
    println("Starting")
    
    // compile shaders
    val sf = new ShaderFactory()
       .setShader(GL_VERTEX_SHADER, new File(shaderPath, "vert.glsl"))
       .setShader(GL_FRAGMENT_SHADER, new File(shaderPath, "frag.glsl"))
       .registerAttribute("pos")
       .registerAttribute("model")
       
     sf.buildProgram() match {
       case Success(p) => {
         prog = p
       }
       
       case Failure(e) => {
         return SceneError(e)
       }
    }
    
    sf.cleanUp()
    
    
    
    // push data onto gpu
    
    val iboId = glGenBuffers()
    glBindBuffer(GL_DRAW_INDIRECT_BUFFER, iboId)
    
    val ibuf = BufferUtils.createIntBuffer(indirectData.length)
    ibuf.put(indirectData).flip()
    glBufferData(GL_DRAW_INDIRECT_BUFFER, ibuf, GL_STATIC_DRAW)
    
    vaoId = glGenVertexArrays()
    glBindVertexArray(vaoId)
    
    val vertexBufferId = glGenBuffers()
    glBindBuffer(GL_ARRAY_BUFFER, vertexBufferId)
    
    val vbuf = BufferUtils.createFloatBuffer(vertexData.length)
    vbuf.put(vertexData).flip()
    glBufferData(GL_ARRAY_BUFFER, vbuf, GL_STATIC_DRAW)
    
    val posAttribLoc = prog.attribLocations("pos")
    
    glVertexAttribPointer(posAttribLoc, 3, GL_FLOAT, false, 0, 0)
    
    
    
    mboId = glGenBuffers()
    glBindBuffer(GL_ARRAY_BUFFER, mboId)
    
    val mbuf = BufferUtils.createFloatBuffer(modelData.length)
    mbuf.put(modelData).flip()
    glBufferData(GL_ARRAY_BUFFER, mbuf, GL_STATIC_DRAW)
    
    val modAttribLoc = prog.attribLocations("model")
    
    val bytePerFloat = 4
    val floatPerMat = 16
    val stride = bytePerFloat * floatPerMat
    
    // mat4 attribute = 4 vec4 attributes
    glVertexAttribPointer(modAttribLoc,   4, GL_FLOAT, false, stride, 0)
    glVertexAttribPointer(modAttribLoc+1, 4, GL_FLOAT, false, stride, 4*bytePerFloat)
    glVertexAttribPointer(modAttribLoc+2, 4, GL_FLOAT, false, stride, 8*bytePerFloat)
    glVertexAttribPointer(modAttribLoc+3, 4, GL_FLOAT, false, stride, 12*bytePerFloat)
    
    // one matrix per instance, not per vertex
    glVertexAttribDivisor(modAttribLoc,   1)
    glVertexAttribDivisor(modAttribLoc+1, 1)
    glVertexAttribDivisor(modAttribLoc+2, 1)
    glVertexAttribDivisor(modAttribLoc+3, 1)
    
//    glBindBuffer(GL_ARRAY_BUFFER, 0)
//    glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0)
    
    glEnableVertexAttribArray(posAttribLoc)
    
    glEnableVertexAttribArray(modAttribLoc)
    glEnableVertexAttribArray(modAttribLoc+1)
    glEnableVertexAttribArray(modAttribLoc+2)
    glEnableVertexAttribArray(modAttribLoc+3)
    
    glUseProgram(prog.id)
    
    GlUtils.printIfError()
    println("Start done")
    
    SceneSuccess
  }
  
  def update(dt: Float): SceneResult = {
    // DRAW!
    
    glMultiDrawArraysIndirect(GL_TRIANGLES, 0, indirectData.length/4, 16)
    
//    GL31.glDrawArraysInstanced(GL_TRIANGLES, 0, 3, 1)
    
    GlUtils.printIfError()
    SceneSuccess
  }
  
  def end() = {
    println("Ending")
    
    glUseProgram(0)
    prog.cleanUp()
    
    val modAttribLoc = prog.attribLocations("model")
    
    glDisableVertexAttribArray(modAttribLoc)
    glDisableVertexAttribArray(modAttribLoc+1)
    glDisableVertexAttribArray(modAttribLoc+2)
    glDisableVertexAttribArray(modAttribLoc+3)
    
    glDisableVertexAttribArray(prog.attribLocations("pos"))
    
    glBindVertexArray(0)
    glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0)
    
    glDeleteVertexArrays(vaoId)
    
    glDeleteBuffers(mboId)
    glDeleteBuffers(vboId)
    glDeleteBuffers(iboId)
    
    GlUtils.printIfError()
    println("End done")
    
    // Done!
    SceneSuccess
  }
}