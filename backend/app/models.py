from sqlalchemy import Column, String, Boolean, DateTime, Numeric, Text, JSON, SmallInteger, Index
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
import uuid
from app.database import Base


class AccessEvent(Base):
    __tablename__ = "access_events"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(String(64), nullable=False, index=True)
    pin_hash = Column(String(128), nullable=False)
    success = Column(Boolean, nullable=False)
    timestamp = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)
    source = Column(String(32), nullable=False, default="keypad")

    __table_args__ = (
        Index("idx_access_events_timestamp_desc", timestamp.desc()),
        Index("idx_access_events_user_id", user_id),
    )


class SensorEvent(Base):
    __tablename__ = "sensor_events"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    sensor_id = Column(String(64), nullable=False)
    sensor_type = Column(String(32), nullable=False, index=True)
    value = Column(Numeric(10, 4), nullable=False)
    filtered_value = Column(Numeric(10, 4))
    unit = Column(String(16), nullable=False)
    timestamp = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)
    metadata = Column(JSON, nullable=False, default=dict)

    __table_args__ = (
        Index("idx_sensor_events_timestamp_desc", timestamp.desc()),
        Index("idx_sensor_events_sensor_type", sensor_type),
    )


class SecurityEvent(Base):
    __tablename__ = "security_events"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    event_type = Column(String(32), nullable=False, index=True)
    severity = Column(String(16), nullable=False, default="medium")
    description = Column(Text)
    timestamp = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)
    acknowledged = Column(Boolean, nullable=False, default=False)
    acknowledged_at = Column(DateTime(timezone=True))
    acknowledged_by = Column(String(64))

    __table_args__ = (
        Index("idx_security_events_timestamp_desc", timestamp.desc()),
        Index("idx_security_events_event_type", event_type),
    )


class RoverTelemetry(Base):
    __tablename__ = "rover_telemetry"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    session_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    left_motor_pwm = Column(SmallInteger, nullable=False)
    right_motor_pwm = Column(SmallInteger, nullable=False)
    ultrasonic_distance_cm = Column(Numeric(6, 2))
    ir_left = Column(Boolean)
    ir_center = Column(Boolean)
    ir_right = Column(Boolean)
    rf_rssi = Column(SmallInteger)
    timestamp = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)

    __table_args__ = (
        Index("idx_rover_telemetry_session_timestamp", session_id, timestamp),
    )