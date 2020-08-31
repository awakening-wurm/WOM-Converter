package com.wurmonline.womconverter.converters;

import com.google.common.io.LittleEndianDataInputStream;
import com.wurmonline.womconverter.MatReporter;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class WOMToDAEConverter {

    private static final String FLOATS_FORMAT = "%.6f";

    private static class Vector3D {
        float x = 0.0f;
        float y = 0.0f;
        float z = 0.0f;
    }

    private static class Color {
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 0.0f;
    }

    private static class Vertex {
        Vector3D vertex = null;
        Vector3D normal = null;
        Vector3D uv = null;
        Color color = null;
        Vector3D tangent = null;
        Vector3D binormal = null;
    }

    private static class Face {
        short a = 0;
        short b = 0;
        short c = 0;
    }

    private static class Material {
        String textureName = null;
        String materialName = null;
        Color emissive = null;
        float shininess = 0.0f;
        Color specular = null;
        Color transparency = null;
    }

    private static class Mesh {
        String name = null;
        Vertex[] vertices = null;
        Face[] faces = null;
        Material[] materials = null;
    }

    public static void convert(File inputFile, File outputDirectory, boolean generateTangents, Properties forceMats, MatReporter matReport) throws MalformedURLException, IOException {
        if (inputFile == null || outputDirectory == null) {
            throw new IllegalArgumentException("Input file and/or output directory cannot be null");
        } else if (!outputDirectory.isDirectory()) {
            throw new IllegalArgumentException("Output directory is not a directory");
        }

        String modelFileName = inputFile.getName();
        modelFileName = modelFileName.substring(0, modelFileName.lastIndexOf('.'));
        File outputFile = new File(outputDirectory, modelFileName + ".dae");

        System.out.println("------------------------------------------------------------------------");
        System.out.println("Converting file: " + inputFile.getName() + " to: " + outputFile.getAbsolutePath());

        if(outputFile.exists()) {
            System.out.println("Output file already exists.");
            return;
        }

        LittleEndianDataInputStream input = new LittleEndianDataInputStream(new FileInputStream(inputFile));

        int meshesCount = input.readInt();
        Mesh[] meshes = new Mesh[meshesCount];
        Map<String,Material> materials = new HashMap<>();

        for (int i=0; i<meshesCount; ++i) {
            meshes[i] = readMesh(input);

            int materialCount = input.readInt();
            Material[] meshMmaterials = new Material[materialCount];
            meshes[i].materials = meshMmaterials;
            for(int j=0; j<materialCount; ++j) {
                Material material = readMaterial(input,forceMats,matReport);
                meshMmaterials[j] = material;
                materials.put(material.materialName,material);
            }
        }

        int jointsCount = input.readInt();
        // joint importing here

        for (int i = 0; i < meshesCount; i++) {
            boolean hasSkinning = input.read() == 1;
            // skinning importing here
        }

        input.close();

        try(final OutputStream out = new FileOutputStream(outputFile)) {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
              .append("<COLLADA xmlns=\"http://www.collada.org/2005/11/COLLADASchema\" version=\"1.4.1\">\n")
              .append("  <asset>\n")
              .append("    <contributor>\n")
              .append("      <author>").append("Arkonik").append("</author>\n")
              .append("      <authoring_tool>").append("FBX COLLADA exporter").append("</authoring_tool>\n")
              .append("      <comments></comments>\n")
              .append("    </contributor>\n")
              .append("    <created>").append("2015-11-20T15:27:18Z").append("</created>\n")
              .append("    <keywords></keywords>\n")
              .append("    <modified>").append("2015-11-20T15:27:18Z").append("</modified>\n")
              .append("    <revision></revision>\n")
              .append("    <subject></subject>\n")
              .append("    <title></title>\n")
              .append("    <unit meter=\"1.000000\" name=\"centimeter\"></unit>\n")
              .append("    <up_axis>Y_UP</up_axis>\n")
              .append("  </asset>\n")
              .append("  <library_images>\n");
            for(Map.Entry<String,Material> entry : materials.entrySet()) {
                Material material = entry.getValue();
                sb.append("    <image id=\"").append(material.materialName).append("-image\" name=\"").append(material.materialName).append("\"><init_from>").append(material.textureName).append("</init_from></image>\n");
            }
            sb.append("  </library_images>\n")
              .append("  <library_materials>\n");
            for(Map.Entry<String,Material> entry : materials.entrySet()) {
                Material material = entry.getValue();
                sb.append("    <material id=\"").append(material.materialName).append("1F\" name=\"").append(material.materialName).append("1F\">\n")
                  .append("      <instance_effect url=\"#").append(material.materialName).append("1F-fx\"/>\n")
                  .append("    </material>\n");
            }
            sb.append("  </library_materials>\n")
              .append("  <library_effects>\n");
            for(Map.Entry<String,Material> entry : materials.entrySet()) {
                Material material = entry.getValue();
                sb.append("    <effect id=\"").append(material.materialName).append("1F-fx\" name=\"").append(material.materialName).append("1F\">\n")
                  .append("      <profile_COMMON>\n")
                  .append("        <technique sid=\"standard\">\n")
                  .append("          <phong>\n")
                  .append("            <emission>\n")
                  .append("              <color sid=\"emission\">")
                  .append(String.format(FLOATS_FORMAT,material.emissive.r)).append("  ")
                  .append(String.format(FLOATS_FORMAT,material.emissive.g)).append(" ")
                  .append(String.format(FLOATS_FORMAT,material.emissive.b)).append(" ")
                  .append(String.format(FLOATS_FORMAT,material.emissive.a)).append("</color>\n")
                  .append("            </emission>\n")
                  .append("            <ambient>\n")
                  .append("              <color sid=\"ambient\">1.000000  1.000000 1.000000 1.000000</color>\n")
                  .append("            </ambient>\n")
                  .append("            <diffuse>\n")
                  .append("              <texture texture=\"").append(material.materialName).append("-image\" texcoord=\"CHANNEL0\">\n")
                  .append("                <extra>\n")
                  .append("                  <technique profile=\"MAYA\">\n")
                  .append("                    <wrapU sid=\"wrapU0\">TRUE</wrapU>\n")
                  .append("                    <wrapV sid=\"wrapV0\">TRUE</wrapV>\n")
                  .append("                    <blend_mode>NONE</blend_mode>\n")
                  .append("                  </technique>\n")
                  .append("                </extra>\n")
                  .append("              </texture>\n")
                  .append("            </diffuse>\n")
                  .append("            <specular>\n")
                  .append("              <color sid=\"specular\">")
                  .append(String.format(FLOATS_FORMAT,material.specular.r)).append("  ")
                  .append(String.format(FLOATS_FORMAT,material.specular.g)).append(" ")
                  .append(String.format(FLOATS_FORMAT,material.specular.b)).append(" ")
                  .append(String.format(FLOATS_FORMAT,material.specular.a)).append("</color>\n")
                  .append("            </specular>\n")
                  .append("            <shininess>\n")
                  .append("              <float sid=\"shininess\">")
                  .append(String.format(FLOATS_FORMAT,material.shininess)).append("</float>\n")
                  .append("            </shininess>\n")
                  .append("            <reflective>\n")
                  .append("              <color sid=\"reflective\">0.000000  0.000000 0.000000 1.000000</color>\n")
                  .append("            </reflective>\n")
                  .append("            <reflectivity>\n")
                  .append("              <float sid=\"reflectivity\">0.000000</float>\n")
                  .append("            </reflectivity>\n")
                  .append("            <transparent opaque=\"RGB_ZERO\">\n")
                  .append("              <color sid=\"transparent\">")
                  .append(String.format(FLOATS_FORMAT,material.transparency.r)).append("  ")
                  .append(String.format(FLOATS_FORMAT,material.transparency.g)).append(" ")
                  .append(String.format(FLOATS_FORMAT,material.transparency.b)).append(" ")
                  .append(String.format(FLOATS_FORMAT,material.transparency.a)).append("</color>\n")
                  .append("            </transparent>\n")
                  .append("            <transparency>\n")
                  .append("              <float sid=\"transparency\">1.000000</float>\n")
                  .append("            </transparency>\n")
                  .append("          </phong>\n")
                  .append("        </technique>\n")
                  .append("      </profile_COMMON>\n")
                  .append("    </effect>\n");
            }
            sb.append("  </library_effects>\n")
              .append("  <library_geometries>\n");
            for(Mesh mesh : meshes) {
                int verticesCount = 0;
                int normalsCount = 0;
                int uvCount = 0;
                for(Vertex vertex : mesh.vertices) {
                    if(vertex.vertex!=null) verticesCount++;
                    if(vertex.normal!=null) normalsCount++;
                    if(vertex.uv!=null) uvCount++;
                }
                sb.append("    <geometry id=\"").append(mesh.name).append("-lib\" name=\"").append(mesh.name).append("Mesh\">\n")
                  .append("      <mesh>\n")
                  .append("        <source id=\"").append(mesh.name).append("-POSITION\">\n")
                  .append("          <float_array id=\"").append(mesh.name).append("-POSITION-array\" count=\"").append(verticesCount*3).append("\">\n");
                for(Vertex vertex : mesh.vertices) {
                    sb.append(String.format(FLOATS_FORMAT,vertex.vertex.x)).append(" ")
                      .append(String.format(FLOATS_FORMAT,vertex.vertex.y)).append(" ")
                      .append(String.format(FLOATS_FORMAT,vertex.vertex.z)).append("\n");
                }
                sb.append("</float_array>\n")
                  .append("          <technique_common>\n")
                  .append("            <accessor source=\"#").append(mesh.name).append("-POSITION-array\" count=\"").append(verticesCount).append("\" stride=\"3\">\n")
                  .append("              <param name=\"X\" type=\"float\"/>\n")
                  .append("              <param name=\"Y\" type=\"float\"/>\n")
                  .append("              <param name=\"Z\" type=\"float\"/>\n")
                  .append("            </accessor>\n")
                  .append("          </technique_common>\n")
                  .append("        </source>\n")
                  .append("        <source id=\"").append(mesh.name).append("-Normal0\">\n")
                  .append("          <float_array id=\"").append(mesh.name).append("-Normal0-array\" count=\"").append(normalsCount*3).append("\">\n" );
                for(Vertex vertex : mesh.vertices) {
                    sb.append(String.format(FLOATS_FORMAT,vertex.normal.x)).append(" ")
                      .append(String.format(FLOATS_FORMAT,vertex.normal.y)).append(" ")
                      .append(String.format(FLOATS_FORMAT,vertex.normal.z)).append("\n");
                }
                sb.append("</float_array>\n")
                  .append("          <technique_common>\n")
                  .append("            <accessor source=\"#").append(mesh.name).append("-Normal0-array\" count=\"").append(normalsCount).append("\" stride=\"3\">\n")
                  .append("              <param name=\"X\" type=\"float\"/>\n")
                  .append("              <param name=\"Y\" type=\"float\"/>\n")
                  .append("              <param name=\"Z\" type=\"float\"/>\n")
                  .append("            </accessor>\n")
                  .append("          </technique_common>\n")
                  .append("        </source>\n")
                  .append("        <source id=\"").append(mesh.name).append("-UV0\">\n")
                  .append("          <float_array id=\"").append(mesh.name).append("-UV0-array\" count=\"").append(uvCount*2).append("\">\n" );
                for(Vertex vertex : mesh.vertices) {
                    sb.append(String.format(FLOATS_FORMAT,vertex.uv.x)).append(" ")
                      .append(String.format(FLOATS_FORMAT,vertex.uv.y)).append("\n");
                }
                sb.append("</float_array>\n")
                  .append("          <technique_common>\n")
                  .append("            <accessor source=\"#").append(mesh.name).append("-UV0-array\" count=\"").append(uvCount).append("\" stride=\"2\">\n")
                  .append("              <param name=\"S\" type=\"float\"/>\n")
                  .append("              <param name=\"T\" type=\"float\"/>\n")
                  .append("            </accessor>\n")
                  .append("          </technique_common>\n")
                  .append("        </source>\n")
                  .append("        <vertices id=\"").append(mesh.name).append("-VERTEX\">\n")
                  .append("          <input semantic=\"POSITION\" source=\"#").append(mesh.name).append("-POSITION\"/>\n")
                  .append("        </vertices>\n")
                  .append("        <triangles count=\"").append(mesh.faces.length).append("\" material=\"").append(mesh.materials[0].materialName).append("1F\">\n")
                  .append("          <input semantic=\"VERTEX\" offset=\"0\" source=\"#").append(mesh.name).append("-VERTEX\"/>\n")
                  .append("          <input semantic=\"NORMAL\" offset=\"1\" source=\"#").append(mesh.name).append("-Normal0\"/>\n")
                  .append("          <input semantic=\"TEXCOORD\" offset=\"2\" set=\"0\" source=\"#").append(mesh.name).append("-UV0\"/><p>");
                for(Face face : mesh.faces) {
                    sb.append(" ").append(face.a).append(" ").append(face.b).append(" ").append(face.b);
                }
                sb.append("</p></triangles>\n")
                  .append("      </mesh>\n")
                  .append("    </geometry>\n");
            }
            sb.append("  </library_geometries>\n")
              .append("  <library_visual_scenes>\n");
            for(Mesh mesh : meshes) {
                sb.append("    <visual_scene id=\"").append(mesh.name).append("\" name=\"").append(mesh.name).append("\">\n")
                  .append("      <node name=\"").append(mesh.name).append("\" id=\"").append(mesh.name).append("\" sid=\"").append(mesh.name).append("\">\n")
                  .append("        <matrix sid=\"matrix\">")
                  .append("-1.000000 0.000000 0.000000 0.000000 0.000000 1.000000 -0.000000 0.000000 -0.000000 -0.000000 -1.000000 0.000000 0.000000 0.000000 0.000000 1.000000")
                  .append("</matrix>\n")
                  .append("        <instance_geometry url=\"#").append(mesh.name).append("-lib\">\n")
                  .append("          <bind_material>\n")
                  .append("            <technique_common>\n")
                  .append("              <instance_material symbol=\"").append(mesh.materials[0].materialName).append("1F\" target=\"#").append(mesh.materials[0].materialName).append("1F\"/>\n")
                  .append("            </technique_common>\n")
                  .append("          </bind_material>\n")
                  .append("        </instance_geometry>\n")
                  .append("        <extra>\n")
                  .append("          <technique profile=\"FCOLLADA\"><visibility>1.000000</visibility></technique>\n")
                  .append("        </extra>\n")
                  .append("      </node>\n")
                  .append("      <extra>")
                  .append("        <technique profile=\"MAX3D\">")
                  .append("          <frame_rate>30.000000</frame_rate>")
                  .append("        </technique>")
                  .append("        <technique profile=\"FCOLLADA\">")
                  .append("          <start_time>0.000000</start_time>")
                  .append("          <end_time>3.333333</end_time>")
                  .append("        </technique>")
                  .append("      </extra>\n")
                  .append("    </visual_scene>\n");
            }
            sb.append("  </library_visual_scenes>\n")
              .append("  <scene>\n");
            for(Mesh mesh : meshes) {
                sb.append("    <instance_visual_scene url=\"#").append(mesh.name).append("\"></instance_visual_scene>\n");
            }
            sb.append("  </scene>\n")
              .append("</COLLADA>\n");
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }

        System.out.println("Done.");

        if (matReport != null) matReport.reportFile(inputFile.getName());
    }

    private static Mesh readMesh(LittleEndianDataInputStream input) throws IOException {
        boolean hasTangents = input.read() == 1;
        boolean hasBinormal = input.read() == 1;
        boolean hasVertexColor = input.read() == 1;

        Mesh mesh = new Mesh();
        mesh.name = readString(input);
        System.out.println("Mesh name:\t" + mesh.name);

        System.out.println("Has tangents:\t" + hasTangents);
        System.out.println("Has binormals:\t" + hasBinormal);
        System.out.println("Has colors:\t" + hasVertexColor);

        int verticesCount = input.readInt();
        System.out.println("Vertices:\t" + verticesCount);
        mesh.vertices = new Vertex[verticesCount];
        for (int i = 0; i < verticesCount; i++) {
            Vertex vertex = new Vertex();
            mesh.vertices[i] = vertex;

            vertex.vertex = new Vector3D();
            vertex.vertex.x = input.readFloat();
            vertex.vertex.y = input.readFloat();
            vertex.vertex.z = input.readFloat();

            vertex.normal = new Vector3D();
            vertex.normal.x = input.readFloat();
            vertex.normal.y = input.readFloat();
            vertex.normal.z = input.readFloat();

            vertex.uv = new Vector3D();
            vertex.uv.x = input.readFloat();
            vertex.uv.y = 1 - input.readFloat();

            if (hasVertexColor) {
                vertex.color = new Color();
                vertex.color.r = input.readFloat();
                vertex.color.g = input.readFloat();
                vertex.color.b = input.readFloat();
            }

            if (hasTangents) {
                vertex.tangent = new Vector3D();
                vertex.tangent.x = input.readFloat();
                vertex.tangent.y = input.readFloat();
                vertex.tangent.z = input.readFloat();
            }

            if (hasBinormal) {
                vertex.binormal = new Vector3D();
                vertex.binormal.x = input.readFloat();
                vertex.binormal.y = input.readFloat();
                vertex.binormal.z = input.readFloat();
            }
        }

        int facesCount = input.readInt() / 3;
        System.out.println("Faces:\t\t" + facesCount);
        System.out.println("Triangles:\t" + (facesCount * 3));
        mesh.faces = new Face[facesCount];
        for (int i = 0; i < facesCount; i++) {
            Face face = new Face();
            mesh.faces[i] = face;
            face.a = input.readShort();
            face.b = input.readShort();
            face.c = input.readShort();
        }

        System.out.println("");
        return mesh;
    }

    private static Material readMaterial(LittleEndianDataInputStream input,Properties forceMats,MatReporter matReport) throws IOException {
        Material material = new Material();
        material.textureName = readString(input);
        material.materialName = readString(input);
        if(matReport!=null) matReport.addMat(material.materialName,material.textureName);
        System.out.println("Material name:\t"+material.materialName);
        System.out.println("Texture path:\t"+material.textureName);
        boolean isEnabled = input.read() == 1;
        boolean propertyExists = true;
        propertyExists = input.read() == 1;
        material.emissive = new Color();
        material.emissive.r = input.readFloat();
        material.emissive.g = input.readFloat();
        material.emissive.b = input.readFloat();
        material.emissive.a = input.readFloat();
        System.out.println("Emissive:\t"+
                           String.format(FLOATS_FORMAT,material.emissive.r)+"\t"+
                           String.format(FLOATS_FORMAT,material.emissive.g)+"\t"+
                           String.format(FLOATS_FORMAT,material.emissive.b)+"\t"+
                           String.format(FLOATS_FORMAT,material.emissive.a));

        propertyExists = input.read() == 1;
        material.shininess = input.readFloat();
        System.out.println("Shininess:\t"+String.format(FLOATS_FORMAT,material.shininess));

        propertyExists = input.read() == 1;
        material.specular = new Color();
        material.specular.r = input.readFloat();
        material.specular.g = input.readFloat();
        material.specular.b = input.readFloat();
        material.specular.a = input.readFloat();
        System.out.println("Specular:\t"+
                           String.format(FLOATS_FORMAT,material.specular.r)+"\t"+
                           String.format(FLOATS_FORMAT,material.specular.g)+"\t"+
                           String.format(FLOATS_FORMAT,material.specular.b)+"\t"+
                           String.format(FLOATS_FORMAT, material.specular.a));

        propertyExists = input.read() == 1;
        material.transparency = new Color();
        material.transparency.r = input.readFloat();
        material.transparency.g = input.readFloat();
        material.transparency.b = input.readFloat();
        material.transparency.a = input.readFloat();
        System.out.println("Transparency:\t"+
                           String.format(FLOATS_FORMAT,material.transparency.r)+"\t"+
                           String.format(FLOATS_FORMAT,material.transparency.g)+"\t"+
                           String.format(FLOATS_FORMAT,material.transparency.b)+"\t"+
                           String.format(FLOATS_FORMAT, material.transparency.a));

        System.out.println("");
        return material;
    }

    private static String readString(LittleEndianDataInputStream input) throws IOException {
        int length = input.readInt();
        byte[] chars = new byte[length];
        for(int i=0; i<length; ++i) {
            chars[i] = input.readByte();
        }
        return new String(chars,"UTF-8");
    }

}
