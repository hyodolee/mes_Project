variable "aws_region" {
  default = "ap-northeast-2"
}

variable "instance_type" {
  default = "t3.small"
}

variable "key_name" {
  description = "AWS 콘솔에 등록된 키 페어 이름 (.pem 파일명에서 확장자 제거)"
}

variable "duckdns_token" {
  description = "DuckDNS 토큰"
}

variable "duckdns_subdomain" {
  description = "DuckDNS 서브도메인 (hyodo)"
}

variable "email" {
  description = "Let's Encrypt 인증서 발급용 이메일"
}
