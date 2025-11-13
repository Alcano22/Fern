#type vertex
#version 330 core

layout(location = 0) in vec2 a_Position;
layout(location = 1) in vec2 a_UV;

out vec2 v_UV;

uniform mat4 u_MVP;

void main()
{
    v_UV = a_UV;

    gl_Position = u_MVP * vec4(a_Position, 0.0, 1.0);
}

#type fragment
#version 330 core

in vec2 v_UV;

out vec4 FragColor;

uniform sampler2D u_Texture;
uniform bool u_UseTexture;
uniform vec4 u_Tint;

void main()
{
    if (u_UseTexture)
        FragColor = texture(u_Texture, v_UV) * u_Tint;
    else
        FragColor = u_Tint;
}
