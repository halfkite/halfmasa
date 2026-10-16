"""Test the shipped Servux codec against real Minecraft, Netty and MaLiLib dependencies."""
import os
from pathlib import Path
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
HARNESS = r'''
package io.github.halfmasa.xaerobinding.feature;
import java.util.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.UUIDUtil;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.util.DataByteBufUtils;
public class DespawnProtocolCheck {
 static int checks;
 static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);checks++;}
 static FriendlyByteBuf buffer(){return new FriendlyByteBuf(Unpooled.buffer());}
 static void invalid(FriendlyByteBuf input,String reason){try{check(ItemDespawnServuxClient.Payload.CODEC.decode(input).problem()!=null,reason);check(!input.isReadable(),"unconsumed invalid input");}finally{input.release();}}
 public static void main(String[] args)throws Exception{
  var id=UUID.randomUUID();var data=new CompoundData().putShort("Age",(short)5900).putCodec("UUID",UUIDUtil.CODEC,id);
  check(ItemDespawnDataBridge.readAge(data)==5900,"short Age not accepted");
  check(ItemDespawnDataBridge.matchesUuid(data,id),"UUID cannot decode");
  check(!ItemDespawnDataBridge.matchesUuid(data,UUID.randomUUID()),"foreign UUID accepted");
  check(ItemDespawnDataBridge.readAge(new CompoundData())==null,"missing Age");
  check(ItemDespawnDataBridge.readAge(new CompoundData().putDouble("Age",Double.NaN))==null,"NaN Age");
  check(ItemDespawnDataBridge.readAge(new CompoundData().putFloat("Age",2.5F))==null,"fractional Age");
  check(ItemDespawnDataBridge.readAge(new CompoundData().putLong("Age",Long.MAX_VALUE))==null,"overflow Age");
  check(ItemDespawnDataBridge.readAge(new CompoundData().putShort("Age",Short.MIN_VALUE))==-32768,"persistent Age");
  var input=buffer();
  try{input.writeVarInt(6);input.writeVarInt(123);DataByteBufUtils.toByteBuf(input,data,CompoundData.TAG_NAME);
   var payload=ItemDespawnServuxClient.Payload.CODEC.decode(input);
   check(payload.kind()==6&&payload.entityId()==123&&payload.problem()==null,"ordinary reply header: "+payload.problem());
   check(payload.data().getInt("Age")==5900,"ordinary reply data");
   check(ItemDespawnDataBridge.matchesUuid(payload.data(),id),"wire UUID lost");
  }finally{input.release();}
  var vanilla=fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt.toVanillaCompound(data);
  var namedBytes=new java.io.ByteArrayOutputStream();try(var output=new java.io.DataOutputStream(namedBytes)){net.minecraft.nbt.NbtIo.write(vanilla,output);}
  input=buffer();try{input.writeVarInt(6);input.writeVarInt(123);input.writeBytes(namedBytes.toByteArray());check(ItemDespawnServuxClient.Payload.CODEC.decode(input).problem()==null,"uncompressed named NBT envelope");}finally{input.release();}
  var compressedBytes=new java.io.ByteArrayOutputStream();try(var output=new java.io.DataOutputStream(new java.util.zip.GZIPOutputStream(compressedBytes))){net.minecraft.nbt.NbtIo.write(vanilla,output);}
  input=buffer();try{input.writeVarInt(6);input.writeVarInt(123);input.writeInt(compressedBytes.size());input.writeBytes(compressedBytes.toByteArray());check(ItemDespawnServuxClient.Payload.CODEC.decode(input).problem()==null,"length-prefixed GZIP envelope");}finally{input.release();}
  var metadata=new CompoundTag();metadata.putInt("version",2);metadata.putString("servux","servux-fabric-26.3-0.12.1");
  input=buffer();try{input.writeVarInt(1);input.writeNbt(metadata);var decoded=ItemDespawnServuxClient.Payload.CODEC.decode(input);check(decoded.metadata()!=null&&decoded.problem()==null,"metadata response");}finally{input.release();}
  input=buffer();try{ItemDespawnServuxClient.Payload.CODEC.encode(input,new ItemDespawnServuxClient.Payload(2,-1,metadata,null,null));check(input.readVarInt()==2,"metadata request type");check(input.readNbt().equals(metadata),"metadata request data");}finally{input.release();}
  input=buffer();try{ItemDespawnServuxClient.Payload.CODEC.encode(input,new ItemDespawnServuxClient.Payload(4,123,null,null,null));check(input.readVarInt()==4&&input.readVarInt()==123&&!input.isReadable(),"entity request");}finally{input.release();}
  invalid(buffer(),"empty packet");
  input=buffer();input.writeByte(128);invalid(input,"truncated VarInt");
  input=buffer();input.writeBytes(new byte[]{-1,-1,-1,-1,-1,0});invalid(input,"oversize VarInt");
  input=buffer();input.writeVarInt(6);invalid(input,"missing entity ID");
  input=buffer();input.writeVarInt(6);input.writeVarInt(-1);input.writeInt(1);input.writeByte(0);invalid(input,"negative entity ID");
  input=buffer();input.writeVarInt(6);input.writeVarInt(1);input.writeInt(-1);invalid(input,"negative data length");
  input=buffer();input.writeVarInt(6);input.writeVarInt(1);input.writeInt(100);input.writeByte(0);invalid(input,"length exceeds input");
  input=buffer();input.writeVarInt(6);input.writeVarInt(1);input.writeInt(1);input.writeBytes(new byte[]{0,0});invalid(input,"trailing data");
  for(int type:new int[]{10,11,100}){input=buffer();input.writeVarInt(type);input.writeBytes(new byte[]{1,2,3});invalid(input,"splitter/unknown bytes treated as Age");}
  // A large, valid component-like payload still travels in an ordinary compressed reply.
  var large=new CompoundData().putShort("Age",(short)100).putCodec("UUID",UUIDUtil.CODEC,id).putByteArray("component_blob",new byte[300000]);
  input=buffer();try{input.writeVarInt(6);input.writeVarInt(1);DataByteBufUtils.toByteBuf(input,large,CompoundData.TAG_NAME);var decoded=ItemDespawnServuxClient.Payload.CODEC.decode(input);check(decoded.problem()==null&&decoded.data().getByteArray("component_blob").length==300000,"large ordinary reply");}finally{input.release();}
  System.out.println("DESPAWN_PROTOCOL_PASS checks="+checks);
 }
}
'''


LEGACY_HARNESS = r'''
package io.github.halfmasa.xaerobinding.feature;
import java.util.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.*;
import net.minecraft.core.UUIDUtil;
public class DespawnProtocolCheck {
 static int checks;
 static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);checks++;}
 static FriendlyByteBuf buffer(){return new FriendlyByteBuf(Unpooled.buffer());}
 static void invalid(FriendlyByteBuf b,String reason){try{check(ItemDespawnServuxClient.Payload.CODEC.decode(b).problem()!=null,reason);check(!b.isReadable(),"invalid bytes not consumed");}finally{b.release();}}
 public static void main(String[] args){
  var uuid=UUID.randomUUID();var data=new CompoundTag();data.putShort("Age",(short)5900);data.put("UUID",UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE,uuid).getOrThrow());
  check(ItemDespawnDataBridge.readAge(data)==5900,"short Age");check(ItemDespawnDataBridge.matchesUuid(data,uuid),"UUID");check(!ItemDespawnDataBridge.matchesUuid(data,UUID.randomUUID()),"foreign UUID");
  check(ItemDespawnDataBridge.readAge(new CompoundTag())==null,"missing Age");var malformed=new CompoundTag();malformed.putDouble("Age",Double.NaN);check(ItemDespawnDataBridge.readAge(malformed)==null,"NaN Age");malformed.putFloat("Age",2.5F);check(ItemDespawnDataBridge.readAge(malformed)==null,"fractional Age");malformed.putLong("Age",Long.MAX_VALUE);check(ItemDespawnDataBridge.readAge(malformed)==null,"overflow Age");malformed.putShort("Age",Short.MIN_VALUE);check(ItemDespawnDataBridge.readAge(malformed)==-32768,"persistent Age");
  var b=buffer();try{b.writeVarInt(6);b.writeVarInt(123);b.writeNbt(data);var p=ItemDespawnServuxClient.Payload.CODEC.decode(b);check(p.kind()==6&&p.entityId()==123&&p.problem()==null,"v1 ordinary reply");check(ItemDespawnDataBridge.readAge(p.data())==5900,"v1 Age");check(ItemDespawnDataBridge.matchesUuid(p.data(),uuid),"v1 UUID");}finally{b.release();}
  var meta=new CompoundTag();meta.putInt("version",1);meta.putString("servux","servux-fabric-test");b=buffer();try{b.writeVarInt(1);b.writeNbt(meta);check(ItemDespawnServuxClient.Payload.CODEC.decode(b).metadata().equals(meta),"metadata response");}finally{b.release();}
  b=buffer();try{ItemDespawnServuxClient.Payload.CODEC.encode(b,new ItemDespawnServuxClient.Payload(2,-1,meta,null,null));check(b.readVarInt()==2&&b.readNbt().equals(meta),"metadata request");}finally{b.release();}
  b=buffer();try{ItemDespawnServuxClient.Payload.CODEC.encode(b,new ItemDespawnServuxClient.Payload(4,123,null,null,null));check(b.readVarInt()==4&&b.readVarInt()==123&&!b.isReadable(),"entity request");}finally{b.release();}
  invalid(buffer(),"empty packet");b=buffer();b.writeByte(128);invalid(b,"truncated VarInt");b=buffer();b.writeBytes(new byte[]{-1,-1,-1,-1,-1,0});invalid(b,"oversize VarInt");b=buffer();b.writeVarInt(6);invalid(b,"missing ID");b=buffer();b.writeVarInt(6);b.writeVarInt(-1);b.writeNbt(data);invalid(b,"negative ID");b=buffer();b.writeVarInt(6);b.writeVarInt(1);b.writeByte(10);invalid(b,"truncated NBT");b=buffer();b.writeVarInt(6);b.writeVarInt(1);b.writeNbt(data);b.writeByte(0);invalid(b,"trailing data");
  for(int type:new int[]{10,11,100}){b=buffer();b.writeVarInt(type);b.writeByte(1);invalid(b,"unknown/splitter data");}
  data.putByteArray("component_blob",new byte[300000]);b=buffer();try{b.writeVarInt(6);b.writeVarInt(1);b.writeNbt(data);check(ItemDespawnServuxClient.Payload.CODEC.decode(b).problem()==null,"large v1 NBT reply");}finally{b.release();}
  System.out.println("DESPAWN_PROTOCOL_PASS version="+args[0]+" protocol=1 checks="+checks);
 }
}
'''


def run(version):
    modern = version.startswith("26.") or tuple(map(int, version.split("."))) >= (1, 21, 11)
    classpath = (ROOT / f"build/port-classpath-{version}.txt").read_text(encoding="utf-8")
    classpath = str(ROOT / f"versions/{version}/build/classes/java/main") + os.pathsep + classpath
    java = Path(os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-25.0.3")) / "bin"
    with tempfile.TemporaryDirectory(prefix="halfmasa-despawn-protocol-") as folder:
        work = Path(folder)
        source = work / "DespawnProtocolCheck.java"
        source.write_text(HARNESS if modern else LEGACY_HARNESS, encoding="utf-8")
        result = subprocess.run([str(java / "javac.exe"), "-encoding", "UTF-8", "-cp", classpath, "-d", folder, str(source)], capture_output=True)
        if result.returncode:
            raise RuntimeError(result.stderr.decode(errors="replace"))
        result = subprocess.run([str(java / "java.exe"), "-Dfile.encoding=UTF-8", "-cp", folder + os.pathsep + classpath, "io.github.halfmasa.xaerobinding.feature.DespawnProtocolCheck", version], cwd=folder, capture_output=True, timeout=90)
        if result.returncode:
            raise RuntimeError((result.stdout + result.stderr).decode(errors="replace"))
        print(result.stdout.decode(errors="replace"), end="")


if __name__ == "__main__":
    for version in sys.argv[1:] or ["26.3"]:
        run(version)
